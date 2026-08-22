package com.restart.spacestationtracker.domain.people_in_space.model

data class Astronaut(
    val name: String,
    val craft: String,
    val bio: String,
    val bioUrl: String,
    val profileImageUrl: String,
    val launchDate: Long,
    val role: String,
    val flagCode: String,
    val twitterUrl: String?,
    val instagramUrl: String?,
    val facebookUrl: String?
)
