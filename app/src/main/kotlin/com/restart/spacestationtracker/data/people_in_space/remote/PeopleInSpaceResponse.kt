package com.restart.spacestationtracker.data.people_in_space.remote

import com.google.gson.annotations.SerializedName
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut

data class PeopleInSpaceResponseDto(
    val number: Int,
    val people: List<PersonDto>,
    @SerializedName("iss_expedition") val issExpedition: Int,
)

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
