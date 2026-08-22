package com.restart.spacestationtracker.domain.people_in_space.model

data class Expedition(
    val number: Int,
    val patchUrl: String,
    val url: String,
    val imageUrl: String,
    val startDate: Long,
    val endDate: Long,
    val bio: String,
)
