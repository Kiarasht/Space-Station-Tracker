package com.restart.spacestationtracker.shared.passes

data class PassAlertReminder(
    val notificationTime: String,
    val triggerTimeMillis: Long,
    val identifierSuffix: String
)

object PassAlertPolicy {
    const val AT_EVENT = "At time of event"
    const val TEN_MINUTES_BEFORE = "10 minutes before"
    const val ONE_HOUR_BEFORE = "1 hour before"
    const val TWELVE_HOURS_BEFORE = "12 hours before"
    const val ONE_DAY_BEFORE = "1 day before"
    const val ONE_WEEK_BEFORE = "1 week before"

    val supportedNotificationTimes = listOf(
        AT_EVENT,
        TEN_MINUTES_BEFORE,
        ONE_HOUR_BEFORE,
        TWELVE_HOURS_BEFORE,
        ONE_DAY_BEFORE,
        ONE_WEEK_BEFORE
    )

    fun matchesMinimumVisibility(
        magnitude: Double,
        minimumVisibility: String
    ): Boolean {
        val threshold = when (minimumVisibility) {
            "Very Bright" -> -2.0
            "Bright" -> -1.5
            "Moderate" -> -1.0
            "Faint" -> 0.0
            else -> -1.5
        }
        return magnitude < threshold
    }

    fun remindersFor(
        startTimeMillis: Long,
        notificationTimes: Collection<String>,
        nowMillis: Long
    ): List<PassAlertReminder> {
        return notificationTimes
            .ifEmpty { listOf(TEN_MINUTES_BEFORE) }
            .distinct()
            .mapNotNull { notificationTime ->
                val triggerTimeMillis = startTimeMillis - leadTimeMillis(notificationTime)
                if (triggerTimeMillis <= nowMillis) {
                    null
                } else {
                    PassAlertReminder(
                        notificationTime = notificationTime,
                        triggerTimeMillis = triggerTimeMillis,
                        identifierSuffix = notificationTime
                            .lowercase()
                            .replace(Regex("[^a-z0-9]+"), "_")
                            .trim('_')
                    )
                }
            }
            .sortedBy(PassAlertReminder::triggerTimeMillis)
    }

    private fun leadTimeMillis(notificationTime: String): Long {
        return when (notificationTime) {
            AT_EVENT -> 0L
            TEN_MINUTES_BEFORE -> 10L * 60L * 1_000L
            ONE_HOUR_BEFORE -> 60L * 60L * 1_000L
            TWELVE_HOURS_BEFORE -> 12L * 60L * 60L * 1_000L
            ONE_DAY_BEFORE -> 24L * 60L * 60L * 1_000L
            ONE_WEEK_BEFORE -> 7L * 24L * 60L * 60L * 1_000L
            else -> 0L
        }
    }
}
