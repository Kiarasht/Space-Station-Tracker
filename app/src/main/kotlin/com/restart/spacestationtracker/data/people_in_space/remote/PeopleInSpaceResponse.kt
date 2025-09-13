package com.restart.spacestationtracker.data.people_in_space.remote

import com.google.gson.annotations.SerializedName
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition

data class PeopleInSpaceResponseDto(
    val number: Int,
    val people: List<PersonDto>,
    @SerializedName("iss_expedition") val issExpedition: Int,
    @SerializedName("expedition_patch") val expeditionPatch: String,
    @SerializedName("expedition_url") val expeditionUrl: String,
    @SerializedName("expedition_image") val expeditionImage: String,
    @SerializedName("expedition_start_date") val expeditionStartDate: Long,
    @SerializedName("expedition_end_date") val expeditionEndDate: Long,
) {
    fun toExpedition(): Expedition {
        return Expedition(
            number = issExpedition,
            patchUrl = expeditionPatch,
            url = expeditionUrl,
            imageUrl = expeditionImage,
            startDate = expeditionStartDate,
            endDate = expeditionEndDate,
            bio = "" // Bio will be fetched separately
        )
    }
}

data class PersonDto(
    val id: Int,
    val name: String,
    val country: String,
    @SerializedName("flag_code") val flagCode: String,
    val agency: String,
    val position: String,
    val spacecraft: String,
    val launched: Long,
    val iss: Boolean,
    @SerializedName("days_in_space") val daysInSpace: Int,
    val url: String,
    val image: String,
    val instagram: String?,
    val twitter: String?,
    val facebook: String?
) {
    fun toAstronaut(): Astronaut {
        return Astronaut(
            name = name,
            craft = spacecraft,
            bio = "", // Bio will be fetched separately
            bioUrl = url,
            profileImageUrl = image,
            launchDate = launched,
            role = position,
            flagCode = flagCode,
            twitterUrl = twitter,
            instagramUrl = instagram,
            facebookUrl = facebook
        )
    }
}

data class WikiBioResponseDto(
    val query: QueryDto?
)

data class QueryDto(
    val pages: Map<String, PageDto>?
)

data class PageDto(
    val pageid: Int,
    val ns: Int,
    val title: String,
    val extract: String
)
