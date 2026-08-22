package com.restart.spacestationtracker.shared.network

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.iss_live.repository.IssRepository
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.iss_passes.repository.IssPassesRepository
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.domain.people_in_space.repository.PeopleInSpaceRepository
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.domain.youtube.repository.YouTubeRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.decodeFromString

class KtorSpaceStationRepository(
    private val config: NetworkConfig = NetworkConfig(),
    private val client: HttpClient = createPlatformHttpClient(userAgent = config.userAgent)
) : IssRepository, IssPassesRepository, PeopleInSpaceRepository, YouTubeRepository {

    private var cachedLiveStreams: List<LiveStream>? = null
    private var liveStreamCacheTimestampMillis = 0L

    override suspend fun getIssLocation(): Result<IssLocation> = runCatching {
        client.get("$ISS_API/v1/satellites/$ISS_SATELLITE_ID")
            .body<IssLocationDto>()
            .toDomain()
    }

    override suspend fun getIssFutureLocations(
        timestamps: List<Long>
    ): Result<List<IssLocation>> = runCatching {
        if (timestamps.isEmpty()) return@runCatching emptyList()
        client.get("$ISS_API/v1/satellites/$ISS_SATELLITE_ID/positions") {
            parameter("timestamps", timestamps.joinToString(","))
            parameter("units", "kilometers")
        }.body<List<IssLocationDto>>().map(IssLocationDto::toDomain)
    }

    override suspend fun getPeopleInSpace(): Result<Pair<Expedition, List<Astronaut>>> = runCatching {
        val response = client.get(PEOPLE_IN_SPACE_URL).body<PeopleInSpaceResponseDto>()
        response.toExpedition() to response.people.map(PersonDto::toAstronaut)
    }

    override suspend fun getAstronautBio(pageTitle: String): Result<String> = runCatching {
        val response = client.get(WIKIPEDIA_API) {
            parameter("action", "query")
            parameter("prop", "extracts")
            parameter("format", "json")
            parameter("exintro", true)
            parameter("titles", pageTitle)
        }.body<WikiBioResponseDto>()
        response.query?.pages?.values?.firstOrNull()?.extract
            ?.toPlainText()
            ?.takeIf(String::isNotBlank)
            ?: "Biography not available."
    }

    override suspend fun getIssPasses(
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Result<List<IssPass>> = runCatching {
        check(config.n2yoApiKey.isNotBlank()) {
            "N2YO_API_KEY is not configured for ${PlatformNetworkConfig.platformName}. " +
                "Use local.properties or the N2YO_API_KEY environment variable on Android, " +
                "and Configuration/Local.xcconfig or the Xcode scheme environment on iOS."
        }
        client.get(
            "$N2YO_API/rest/v1/satellite/visualpasses/$ISS_SATELLITE_ID/" +
                "$latitude/$longitude/$altitude/10/300"
        ) {
            parameter("apiKey", config.n2yoApiKey)
        }.body<IssPassesResponseDto>().passes.orEmpty().map(IssPassDto::toDomain)
    }

    override suspend fun getNasaLiveStreams(): List<LiveStream> {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        cachedLiveStreams
            ?.takeIf { now - liveStreamCacheTimestampMillis < LIVE_STREAM_CACHE_MILLIS }
            ?.let { return it }

        val streams = fetchLiveStreamsFromBackend()
            ?: fetchLiveStreamsFromYouTube()
            ?: emptyList()
        cachedLiveStreams = streams
        liveStreamCacheTimestampMillis = now
        return streams
    }

    private suspend fun fetchLiveStreamsFromBackend(): List<LiveStream>? {
        if (config.youtubeLiveStreamsUrl.isBlank()) return null
        return runCatching {
            spaceStationJson.decodeFromString<NasaLiveStreamsResponse>(
                client.get(config.youtubeLiveStreamsUrl).bodyAsText()
            )
                .streams
                .map(NasaLiveStreamDto::toDomain)
        }.getOrNull()
    }

    private suspend fun fetchLiveStreamsFromYouTube(): List<LiveStream>? {
        if (config.youtubeApiKey.isBlank()) return null
        return runCatching {
            client.get(YOUTUBE_SEARCH_API) {
                config.youtubeRequestHeaders.forEach { (name, value) -> header(name, value) }
                parameter("part", "snippet")
                parameter("channelId", NASA_CHANNEL_ID)
                parameter("eventType", "live")
                parameter("type", "video")
                parameter("key", config.youtubeApiKey)
            }.body<YouTubeSearchResponse>().items.map { video ->
                LiveStream(
                    videoId = video.id.videoId,
                    title = video.snippet.title
                )
            }
        }.getOrNull()
    }

    fun close() {
        client.close()
    }

    private companion object {
        const val ISS_SATELLITE_ID = 25544
        const val ISS_API = "https://api.wheretheiss.at"
        const val PEOPLE_IN_SPACE_URL =
            "https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json"
        const val WIKIPEDIA_API = "https://en.wikipedia.org/w/api.php"
        const val N2YO_API = "https://api.n2yo.com"
        const val YOUTUBE_SEARCH_API = "https://www.googleapis.com/youtube/v3/search"
        const val NASA_CHANNEL_ID = "UCLA_DiR1FfKNvjuUpBHmylQ"
        const val LIVE_STREAM_CACHE_MILLIS = 30L * 60 * 1_000
    }
}

private fun String.toPlainText(): String {
    return replace(Regex("<[^>]+>"), " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
