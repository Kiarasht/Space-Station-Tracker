package com.restart.spacestationtracker.data

data class Astronaut(
        val name: String,
        val image: String,
        val isIss: Boolean,
        val flagCode: String,
        val launchDate: Int,
        val role: String,
        val location: String,
        var bio: String,
        val wiki: String,
        val twitter: String,
        val facebook: String,
        val instagram: String
)