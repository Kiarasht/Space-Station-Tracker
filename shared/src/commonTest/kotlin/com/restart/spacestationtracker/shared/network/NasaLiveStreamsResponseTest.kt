package com.restart.spacestationtracker.shared.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NasaLiveStreamsResponseTest {
    private val now = 1_792_224_254_000L
    private val maxAge = 2L * 60 * 60 * 1_000
    private val minute = 60_000L

    private fun decode(json: String) = spaceStationJson.decodeFromString(NasaLiveStreamsResponse.serializer(), json)

    @Test
    fun keepsStreamsConfirmedRecently() {
        val response = decode(
            """{"status":"ok","updatedAtMillis":${now - 5 * minute},"lastSuccessAtMillis":${now - 5 * minute},
              "streams":[{"videoId":"live1","title":"ISS Live"}]}"""
        )
        assertEquals(listOf("live1"), response.currentStreams(now, maxAge).map { it.videoId })
    }

    @Test
    fun dropsStreamsWhenTheWorkflowStoppedRunning() {
        // The owner's Mac was off: the file is a day old.
        val response = decode(
            """{"status":"ok","updatedAtMillis":${now - 24 * 60 * minute},"lastSuccessAtMillis":${now - 24 * 60 * minute},
              "streams":[{"videoId":"live1","title":"ISS Live"}]}"""
        )
        assertTrue(response.currentStreams(now, maxAge).isEmpty())
    }

    @Test
    fun dropsStreamsWhileTheYouTubeCheckKeepsFailing() {
        // The workflow still runs (updatedAtMillis is fresh) but has not confirmed the list for hours.
        val response = decode(
            """{"status":"error","updatedAtMillis":${now - minute},"lastSuccessAtMillis":${now - 5 * 60 * minute},
              "streams":[{"videoId":"live1","title":"ISS Live"}]}"""
        )
        assertTrue(response.currentStreams(now, maxAge).isEmpty())
    }

    @Test
    fun readsFilesWrittenBeforeLastSuccessExisted() {
        val fresh = decode(
            """{"status":"ok","updatedAtMillis":${now - 10 * minute},"streams":[{"videoId":"live1","title":"ISS Live"}]}"""
        )
        val failing = decode(
            """{"status":"error","updatedAtMillis":${now - 10 * minute},"streams":[{"videoId":"live1","title":"ISS Live"}]}"""
        )
        val bare = decode("""{"streams":[{"videoId":"live1","title":"ISS Live"}]}""")

        assertEquals(1, fresh.currentStreams(now, maxAge).size)
        assertTrue(failing.currentStreams(now, maxAge).isEmpty())
        assertTrue(bare.currentStreams(now, maxAge).isEmpty())
    }

    @Test
    fun trustsTheListWhenTheDeviceClockIsBehind() {
        val response = decode(
            """{"status":"ok","lastSuccessAtMillis":${now + 30 * minute},"streams":[{"videoId":"live1","title":"ISS Live"}]}"""
        )
        assertEquals(1, response.currentStreams(now, maxAge).size)
    }
}
