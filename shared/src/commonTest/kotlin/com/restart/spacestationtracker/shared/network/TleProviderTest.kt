package com.restart.spacestationtracker.shared.network

import com.restart.spacestationtracker.shared.preferences.PreferenceStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TleProviderTest {
    private val line1 = "1 25544U 98067A   26289.78971908  .00007803  00000+0  15012-3 0  9995"
    private val line2 = "2 25544  51.6326  58.3278 0006748 271.9059  88.1156 15.48899044590607"
    private val olderLine1 = "1 25544U 98067A   26288.82192346  .00005580  00000+0  10980-3 0  9999"
    private val olderLine2 = "2 25544  51.6317  63.1136 0006755 265.8514  94.1703 15.48879905590452"
    // 2026-10-17, shortly after the element set's epoch.
    private val now = 1_792_224_254_000L
    private val hour = 60L * 60L * 1_000L

    private val celesTrakBody = "ISS (ZARYA)             \n$line1\n$line2\n"
    private val whereTheIssBody =
        """{"id":"25544","name":"iss","header":"ISS (ZARYA)","line1":"$olderLine1","line2":"$olderLine2"}"""

    @Test
    fun fetchesFromCelesTrakAndReusesTheCachedSet() = runTest {
        val requests = mutableListOf<String>()
        val store = MemoryStore()
        val provider = provider(store, { now }) { url ->
            requests += url
            Response(celesTrakBody)
        }

        assertEquals(line1, provider.currentTle().line1)
        assertEquals(line1, provider.currentTle().line1)
        assertEquals(1, requests.size)
        assertEquals(line1, store.strings["iss_tle_line1"])

        // A new provider (next launch) reads the persisted set instead of fetching again.
        val relaunched = provider(store, { now + hour }) { url ->
            requests += url
            Response(celesTrakBody)
        }
        assertEquals(line1, relaunched.currentTle().line1)
        assertEquals(1, requests.size)
    }

    @Test
    fun fallsBackToWhereTheIssWhenCelesTrakFails() = runTest {
        val provider = provider(MemoryStore(), { now }) { url ->
            if ("celestrak" in url) Response(status = HttpStatusCode.ServiceUnavailable) else Response(whereTheIssBody, json = true)
        }

        assertEquals(olderLine1, provider.currentTle().line1)
    }

    @Test
    fun usesTheStaleSetWhenOfflineAndFailsWithoutOne() = runTest {
        var time = now
        var online = true
        val store = MemoryStore()
        val provider = provider(store, { time }) {
            if (online) Response(celesTrakBody) else Response(status = HttpStatusCode.ServiceUnavailable)
        }
        provider.currentTle()

        online = false
        time = now + 2 * 24 * hour
        assertEquals(line1, provider.currentTle().line1)

        val empty = provider(MemoryStore(), { now }) { Response(status = HttpStatusCode.ServiceUnavailable) }
        assertFailsWith<IllegalStateException> { empty.currentTle() }
    }

    private class Response(
        val body: String = "",
        val json: Boolean = false,
        val status: HttpStatusCode = HttpStatusCode.OK
    )

    private fun provider(
        store: PreferenceStore,
        clock: () -> Long,
        respond: (String) -> Response
    ): TleProvider {
        val engine = MockEngine { request ->
            val response = respond(request.url.toString())
            if (response.status != HttpStatusCode.OK) {
                respondError(response.status)
            } else {
                respond(
                    content = response.body,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        if (response.json) "application/json" else "text/plain"
                    )
                )
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(spaceStationJson) }
        }
        return TleProvider(client, store, clock)
    }

    private class MemoryStore : PreferenceStore {
        val strings = mutableMapOf<String, String>()
        private val longs = mutableMapOf<String, Long>()
        override fun getInt(key: String): Int? = null
        override fun putInt(key: String, value: Int) = Unit
        override fun getLong(key: String): Long? = longs[key]
        override fun putLong(key: String, value: Long) { longs[key] = value }
        override fun getBoolean(key: String): Boolean? = null
        override fun putBoolean(key: String, value: Boolean) = Unit
        override fun getString(key: String): String? = strings[key]
        override fun putString(key: String, value: String) { strings[key] = value }
    }
}
