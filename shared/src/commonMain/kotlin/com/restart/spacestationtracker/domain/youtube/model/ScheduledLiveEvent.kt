package com.restart.spacestationtracker.domain.youtube.model

/** A NASA broadcast scheduled on YouTube that hasn't started yet, such as a spacewalk or a launch. */
data class ScheduledLiveEvent(
    val videoId: String,
    val title: String,
    val scheduledStartMillis: Long
)
