package com.restart.spacestationtracker.shared.network

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IssLocationDto(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val visibility: String,
    val footprint: Double,
    @SerialName("solar_lat") val solarLat: Double,
    @SerialName("solar_lon") val solarLon: Double,
    val timestamp: Long,
    val units: String = "kilometers"
) {
    fun toDomain() = IssLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        velocity = velocity,
        visibility = visibility,
        footprint = footprint,
        solarLat = solarLat,
        solarLon = solarLon,
        timestamp = timestamp
    )
}

@Serializable
internal data class PeopleInSpaceResponseDto(
    val number: Int = 0,
    val people: List<PersonDto> = emptyList(),
    @SerialName("iss_expedition") val issExpedition: Int,
    @SerialName("expedition_patch") val expeditionPatch: String = "",
    @SerialName("expedition_url") val expeditionUrl: String = "",
    @SerialName("expedition_image") val expeditionImage: String = "",
    @SerialName("expedition_start_date") val expeditionStartDate: Long = 0,
    @SerialName("expedition_end_date") val expeditionEndDate: Long = 0
) {
    fun toExpedition() = Expedition(
        number = issExpedition,
        patchUrl = expeditionPatch,
        url = expeditionUrl,
        imageUrl = expeditionImage,
        startDate = expeditionStartDate,
        endDate = expeditionEndDate,
        bio = ""
    )
}

@Serializable
internal data class PersonDto(
    val id: Int = 0,
    val name: String,
    val country: String = "",
    @SerialName("flag_code") val flagCode: String = "",
    val agency: String = "",
    val position: String = "",
    val spacecraft: String = "",
    val launched: Long = 0,
    val iss: Boolean = true,
    @SerialName("days_in_space") val daysInSpace: Int = 0,
    val url: String = "",
    val image: String = "",
    val instagram: String? = null,
    val twitter: String? = null,
    val facebook: String? = null
) {
    fun toAstronaut() = Astronaut(
        name = name,
        craft = spacecraft,
        bio = "",
        bioUrl = url,
        profileImageUrl = image,
        launchDate = launched,
        role = position,
        flagCode = flagCode,
        twitterUrl = twitter,
        instagramUrl = instagram,
        facebookUrl = facebook,
        isOnIss = iss
    )
}

@Serializable
internal data class WikiBioResponseDto(val query: WikiQueryDto? = null)

@Serializable
internal data class WikiQueryDto(val pages: List<WikiPageDto>? = null)

/** One page from the query API in `formatversion=2`, where pages are a list and language links name their title. */
@Serializable
internal data class WikiPageDto(val extract: String = "", val langlinks: List<WikiLangLinkDto> = emptyList())

@Serializable
internal data class WikiLangLinkDto(val lang: String = "", val title: String = "")

/**
 * The live-stream list published by the update-youtube-live-streams workflow. The app shows it however old it
 * is: GitHub runs the workflow only every few hours, and NASA's 24/7 ISS feeds stay live for months.
 */
@Serializable
internal data class NasaLiveStreamsResponse(val streams: List<NasaLiveStreamDto> = emptyList())

@Serializable
internal data class NasaLiveStreamDto(
    val videoId: String,
    val title: String
) {
    fun toDomain() = LiveStream(videoId = videoId, title = title)
}

@Serializable
internal data class YouTubeSearchResponse(
    val items: List<YouTubeVideoDto> = emptyList()
)

@Serializable
internal data class YouTubeVideoDto(
    val id: YouTubeVideoIdDto,
    val snippet: YouTubeSnippetDto
)

@Serializable
internal data class YouTubeVideoIdDto(val videoId: String)

@Serializable
internal data class YouTubeSnippetDto(val title: String)
