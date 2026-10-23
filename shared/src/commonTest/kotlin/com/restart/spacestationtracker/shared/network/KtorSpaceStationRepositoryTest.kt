package com.restart.spacestationtracker.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorSpaceStationRepositoryTest {

    @Test
    fun futureLocationsAreDecodedAndMappedBySharedKtorRepository() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.toString().contains("/v1/satellites/25544/positions"))
            assertEquals("100,160", request.url.parameters["timestamps"])
            respond(
                content = """
                    [
                      {
                        "latitude": 10.5,
                        "longitude": -20.25,
                        "altitude": 421.2,
                        "velocity": 27580.0,
                        "visibility": "daylight",
                        "footprint": 4500.0,
                        "solar_lat": 1.0,
                        "solar_lon": 2.0,
                        "timestamp": 100,
                        "units": "kilometers"
                      }
                    ]
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(spaceStationJson)
            }
        }
        val repository = KtorSpaceStationRepository(client = client)

        val locations = repository.getIssFutureLocations(listOf(100, 160)).getOrThrow()

        assertEquals(1, locations.size)
        assertEquals(10.5, locations.single().latitude)
        assertEquals(-20.25, locations.single().longitude)
        repository.close()
    }
}

class UpcomingLiveEventsTest {

    @Test
    fun upcomingBroadcastsAreReadFromThePublishedListSoonestFirst() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "status": "ok",
                      "streams": [{ "videoId": "M3HKLzjvKPc", "title": "Live Video from the ISS" }],
                      "upcoming": [
                        { "videoId": "b", "title": "Launch", "scheduledStartMillis": 2000 },
                        { "videoId": "a", "title": "U.S. Spacewalk 100", "scheduledStartMillis": 1000 }
                      ]
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repository = KtorSpaceStationRepository(
            config = NetworkConfig(youtubeLiveStreamsUrl = "https://example.test/streams.json"),
            client = HttpClient(engine)
        )

        val events = repository.getUpcomingLiveEvents().getOrThrow()

        assertEquals(listOf("a", "b"), events.map { it.videoId })
        assertEquals(1000L, events.first().scheduledStartMillis)
        repository.close()
    }

    @Test
    fun listsWrittenBeforeUpcomingBroadcastsWereAddedHaveNone() = runTest {
        val engine = MockEngine {
            respond(
                content = """{ "status": "ok", "streams": [] }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repository = KtorSpaceStationRepository(
            config = NetworkConfig(youtubeLiveStreamsUrl = "https://example.test/streams.json"),
            client = HttpClient(engine)
        )

        assertEquals(emptyList(), repository.getUpcomingLiveEvents().getOrThrow())
        repository.close()
    }
}
