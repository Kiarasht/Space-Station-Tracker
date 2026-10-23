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
import com.restart.spacestationtracker.shared.passes.Observer
import com.restart.spacestationtracker.shared.passes.PassPredictor
import com.restart.spacestationtracker.shared.passes.Tle
import com.restart.spacestationtracker.shared.satellites.SatelliteElementsSource
import com.restart.spacestationtracker.shared.satellites.TrackedSatellite
import com.restart.spacestationtracker.shared.preferences.PreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.http.decodeURLPart
import kotlinx.serialization.decodeFromString

class KtorSpaceStationRepository(
    private val config: NetworkConfig = NetworkConfig(),
    private val client: HttpClient = createPlatformHttpClient(userAgent = config.userAgent),
    /** Persists the ISS orbital elements between launches; see [ISS_TLE_PREFS_NAME]. */
    tleStore: PreferenceStore? = null,
    /** Language for crew bios; English is the fallback when an article has no translation. */
    private val bioLanguage: () -> String = { wikipediaLanguage(appLanguageCode()) }
) : IssRepository, IssPassesRepository, PeopleInSpaceRepository, YouTubeRepository, SatelliteElementsSource {

    private val tleProvider = TleProvider(client, tleStore, ::nowMillis)
    private val satelliteTleProviders = TrackedSatellite.entries.associateWith { satellite ->
        if (satellite.noradId == ISS_NORAD_ID) tleProvider else TleProvider(client, tleStore, ::nowMillis, satellite.noradId)
    }

    override suspend fun satelliteElements(satellites: List<TrackedSatellite>): Map<TrackedSatellite, Tle> =
        satellites.mapNotNull { satellite ->
            val tle = runCatching { satelliteTleProviders.getValue(satellite).currentTle() }.getOrNull()
            tle?.let { satellite to it }
        }.toMap()

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
        // The feed lists everyone in space. ISS crew come first in the feed's order, then everyone else
        // grouped by spacecraft so each craft gets one section.
        response.toExpedition() to response.people.map(PersonDto::toAstronaut).sortedWith(
            compareByDescending<Astronaut> { it.isOnIss }.thenBy { if (it.isOnIss) "" else it.craft }
        )
    }

    override suspend fun getAstronautBio(pageTitle: String): Result<String> = runCatching {
        // Titles come from English Wikipedia URLs, which may already be percent-encoded.
        val englishTitle = runCatching { pageTitle.decodeURLPart() }.getOrDefault(pageTitle)
        val language = bioLanguage()
        val englishPage = fetchWikiPage(WIKIPEDIA_API, englishTitle, translationFor = language.takeIf { it != "en" })
        // Prefer the article in the app's language, found through the English page's language links.
        val translatedTitle = englishPage?.langlinks?.firstOrNull { it.lang == language && language != "en" }?.title
        val translated = translatedTitle?.let { title ->
            runCatching { fetchWikiPage(wikipediaApi(language), title, translationFor = null) }.getOrNull()
                ?.extract?.toPlainText()?.takeIf(String::isNotBlank)
        }
        translated
            ?: englishPage?.extract?.toPlainText()?.takeIf(String::isNotBlank)
            ?: throw NoSuchElementException("No Wikipedia extract for $pageTitle")
    }

    /** The intro of one article, plus its title in [translationFor]'s Wikipedia when that exists. */
    private suspend fun fetchWikiPage(api: String, title: String, translationFor: String?): WikiPageDto? =
        client.get(api) {
            parameter("action", "query")
            parameter("prop", if (translationFor != null) "extracts|langlinks" else "extracts")
            parameter("format", "json")
            parameter("formatversion", 2)
            parameter("exintro", true)
            parameter("redirects", true)
            if (translationFor != null) parameter("lllang", translationFor)
            parameter("titles", title)
        }.body<WikiBioResponseDto>().query?.pages?.firstOrNull()

    /** Predicts visible passes on the device from the ISS orbital elements; [altitude] is in meters. */
    override suspend fun getIssPasses(
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Result<List<IssPass>> = runCatching {
        val tle = tleProvider.currentTle()
        withContext(Dispatchers.Default) {
            PassPredictor.visiblePasses(
                tle = tle,
                observer = Observer(latitude, longitude, altitude),
                startMillis = nowMillis()
            )
        }
    }

    override suspend fun getNasaLiveStreams(): List<LiveStream> {
        val now = nowMillis()
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
        // A stale list is treated as "nothing live" rather than falling back to the YouTube API, whose
        // per-key quota is shared by every installation.
        return runCatching {
            spaceStationJson.decodeFromString<NasaLiveStreamsResponse>(
                client.get(config.youtubeLiveStreamsUrl).bodyAsText()
            )
                .currentStreams(nowMillis(), LIVE_STREAM_MAX_AGE_MILLIS)
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

    private fun nowMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val ISS_SATELLITE_ID = 25544
        const val ISS_API = "https://api.wheretheiss.at"
        const val PEOPLE_IN_SPACE_URL =
            "https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json"
        const val WIKIPEDIA_API = "https://en.wikipedia.org/w/api.php"
        fun wikipediaApi(language: String) = "https://$language.wikipedia.org/w/api.php"
        const val YOUTUBE_SEARCH_API = "https://www.googleapis.com/youtube/v3/search"
        const val NASA_CHANNEL_ID = "UCLA_DiR1FfKNvjuUpBHmylQ"
        const val LIVE_STREAM_CACHE_MILLIS = 30L * 60 * 1_000
        // The workflow is scheduled every 15 minutes, but GitHub has been starting it only every 2-5 hours.
        // Each run drops streams that ended, so this only bounds how long a list outlives a stopped workflow.
        const val LIVE_STREAM_MAX_AGE_MILLIS = 12L * 60 * 60 * 1_000
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
