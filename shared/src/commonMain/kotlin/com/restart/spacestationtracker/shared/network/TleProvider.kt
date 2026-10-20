package com.restart.spacestationtracker.shared.network

import com.restart.spacestationtracker.shared.passes.Tle
import com.restart.spacestationtracker.shared.preferences.PreferenceStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** Preference file (Android) or user-defaults suite (iOS) that caches orbital elements. */
const val ISS_TLE_PREFS_NAME = "iss_tle"

internal const val ISS_NORAD_ID = 25544

/**
 * Supplies one satellite's orbital elements (TLE), by default the ISS's for pass prediction, from
 * CelesTrak; the ISS also falls back to Where the ISS at?. A fetched element set is reused
 * for [REFRESH_AFTER_MILLIS], in memory and in [store] so it survives restarts, which keeps requests to
 * CelesTrak to a couple per device per day. When both sources fail, a cached set up to
 * [MAX_STALE_MILLIS] old is used; predictions drift by seconds per day as elements age.
 */
internal class TleProvider(
    private val client: HttpClient,
    private val store: PreferenceStore?,
    private val nowMillis: () -> Long,
    private val noradId: Int = ISS_NORAD_ID
) {
    // The ISS keeps its original keys so element sets cached by earlier versions are still read.
    private val keyPrefix = if (noradId == ISS_NORAD_ID) "iss_tle" else "tle_$noradId"
    private val keyLine1 = "${keyPrefix}_line1"
    private val keyLine2 = "${keyPrefix}_line2"
    private val keyFetchedAt = "${keyPrefix}_fetched_at_millis"

    private val mutex = Mutex()
    private var cached: CachedTle? = null

    suspend fun currentTle(): Tle = mutex.withLock {
        val now = nowMillis()
        val existing = cached ?: readStored()?.also { cached = it }
        if (existing != null && now - existing.fetchedAtMillis < REFRESH_AFTER_MILLIS) {
            return@withLock existing.tle
        }

        val fetched = runCatching { fetchFromCelesTrak() }
            .recoverCatching { error -> if (noradId == ISS_NORAD_ID) fetchFromWhereTheIss() else throw error }
        fetched.getOrNull()?.let { tle ->
            val fresh = CachedTle(tle, now)
            cached = fresh
            store?.putString(keyLine1, tle.line1)
            store?.putString(keyLine2, tle.line2)
            store?.putLong(keyFetchedAt, now)
            return@withLock tle
        }

        if (existing != null && now - existing.tle.epochMillis < MAX_STALE_MILLIS) {
            return@withLock existing.tle
        }
        throw IllegalStateException("Unable to load orbital elements for $noradId", fetched.exceptionOrNull())
    }

    private suspend fun fetchFromCelesTrak(): Tle {
        val lines = client.get("$CELESTRAK_URL$noradId").bodyAsText().lines().map(String::trimEnd)
        val line1 = lines.first { it.startsWith("1 $noradId") }
        val line2 = lines.first { it.startsWith("2 $noradId") }
        return Tle(line1, line2)
    }

    private suspend fun fetchFromWhereTheIss(): Tle {
        val response = client.get(WHERE_THE_ISS_URL).body<WhereTheIssTleDto>()
        return Tle(response.line1.trimEnd(), response.line2.trimEnd())
    }

    private fun readStored(): CachedTle? {
        val store = store ?: return null
        val line1 = store.getString(keyLine1) ?: return null
        val line2 = store.getString(keyLine2) ?: return null
        val fetchedAt = store.getLong(keyFetchedAt) ?: return null
        val tle = runCatching { Tle(line1, line2) }.getOrNull() ?: return null
        return CachedTle(tle, fetchedAt)
    }

    private class CachedTle(val tle: Tle, val fetchedAtMillis: Long)

    @Serializable
    private class WhereTheIssTleDto(val line1: String, val line2: String)

    private companion object {
        const val REFRESH_AFTER_MILLIS = 12L * 60L * 60L * 1_000L
        const val MAX_STALE_MILLIS = 14L * 24L * 60L * 60L * 1_000L
        const val CELESTRAK_URL = "https://celestrak.org/NORAD/elements/gp.php?FORMAT=TLE&CATNR="
        const val WHERE_THE_ISS_URL = "https://api.wheretheiss.at/v1/satellites/25544/tles?format=json"
    }
}
