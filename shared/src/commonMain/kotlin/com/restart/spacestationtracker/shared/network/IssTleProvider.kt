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

/** Preference file (Android) or user-defaults suite (iOS) that caches the ISS elements. */
const val ISS_TLE_PREFS_NAME = "iss_tle"

/**
 * Supplies the ISS orbital elements (TLE) that pass prediction runs on. A fetched element set is reused
 * for [REFRESH_AFTER_MILLIS], in memory and in [store] so it survives restarts, which keeps requests to
 * CelesTrak to a couple per device per day. When both sources fail, a cached set up to
 * [MAX_STALE_MILLIS] old is used; predictions drift by seconds per day as elements age.
 */
internal class IssTleProvider(
    private val client: HttpClient,
    private val store: PreferenceStore?,
    private val nowMillis: () -> Long
) {
    private val mutex = Mutex()
    private var cached: CachedTle? = null

    suspend fun currentTle(): Tle = mutex.withLock {
        val now = nowMillis()
        val existing = cached ?: readStored()?.also { cached = it }
        if (existing != null && now - existing.fetchedAtMillis < REFRESH_AFTER_MILLIS) {
            return@withLock existing.tle
        }

        val fetched = runCatching { fetchFromCelesTrak() }
            .recoverCatching { fetchFromWhereTheIss() }
        fetched.getOrNull()?.let { tle ->
            val fresh = CachedTle(tle, now)
            cached = fresh
            store?.putString(KEY_LINE1, tle.line1)
            store?.putString(KEY_LINE2, tle.line2)
            store?.putLong(KEY_FETCHED_AT, now)
            return@withLock tle
        }

        if (existing != null && now - existing.tle.epochMillis < MAX_STALE_MILLIS) {
            return@withLock existing.tle
        }
        throw IllegalStateException("Unable to load ISS orbital elements", fetched.exceptionOrNull())
    }

    private suspend fun fetchFromCelesTrak(): Tle {
        val lines = client.get(CELESTRAK_URL).bodyAsText().lines().map(String::trimEnd)
        val line1 = lines.first { it.startsWith("1 25544") }
        val line2 = lines.first { it.startsWith("2 25544") }
        return Tle(line1, line2)
    }

    private suspend fun fetchFromWhereTheIss(): Tle {
        val response = client.get(WHERE_THE_ISS_URL).body<WhereTheIssTleDto>()
        return Tle(response.line1.trimEnd(), response.line2.trimEnd())
    }

    private fun readStored(): CachedTle? {
        val store = store ?: return null
        val line1 = store.getString(KEY_LINE1) ?: return null
        val line2 = store.getString(KEY_LINE2) ?: return null
        val fetchedAt = store.getLong(KEY_FETCHED_AT) ?: return null
        val tle = runCatching { Tle(line1, line2) }.getOrNull() ?: return null
        return CachedTle(tle, fetchedAt)
    }

    private class CachedTle(val tle: Tle, val fetchedAtMillis: Long)

    @Serializable
    private class WhereTheIssTleDto(val line1: String, val line2: String)

    private companion object {
        const val KEY_LINE1 = "iss_tle_line1"
        const val KEY_LINE2 = "iss_tle_line2"
        const val KEY_FETCHED_AT = "iss_tle_fetched_at_millis"
        const val REFRESH_AFTER_MILLIS = 12L * 60L * 60L * 1_000L
        const val MAX_STALE_MILLIS = 14L * 24L * 60L * 60L * 1_000L
        const val CELESTRAK_URL = "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE"
        const val WHERE_THE_ISS_URL = "https://api.wheretheiss.at/v1/satellites/25544/tles?format=json"
    }
}
