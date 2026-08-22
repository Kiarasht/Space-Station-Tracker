package com.restart.spacestationtracker.shared.policy

import com.restart.spacestationtracker.shared.preferences.PreferenceStore

class AppReviewRepository(
    private val store: PreferenceStore
) {
    fun recordAppOpened(nowMillis: Long) {
        ensureFirstOpenedAtMillis(nowMillis)
        store.putInt(KEY_LAUNCH_COUNT, (store.getInt(KEY_LAUNCH_COUNT) ?: 0) + 1)
    }

    fun recordMeaningfulInteraction(nowMillis: Long) {
        ensureFirstOpenedAtMillis(nowMillis)
        store.putInt(
            KEY_SCREEN_VISIT_COUNT,
            (store.getInt(KEY_SCREEN_VISIT_COUNT) ?: 0) + 1
        )
    }

    fun isEligibleForReviewPrompt(nowMillis: Long): Boolean {
        val firstOpenedAtMillis = ensureFirstOpenedAtMillis(nowMillis)
        if (store.getBoolean(KEY_HAS_RATED) == true) return false
        if (store.getBoolean(KEY_REVIEW_FLOW_COMPLETED) == true) return false
        if (nowMillis < (store.getLong(KEY_SNOOZED_UNTIL) ?: 0L)) return false
        if (nowMillis - firstOpenedAtMillis < MIN_USAGE_AGE_MILLIS) return false

        return (store.getInt(KEY_LAUNCH_COUNT) ?: 0) >= MIN_LAUNCH_COUNT &&
            (store.getInt(KEY_SCREEN_VISIT_COUNT) ?: 0) >= MIN_MEANINGFUL_INTERACTIONS
    }

    fun markReviewFlowCompleted(nowMillis: Long) {
        store.putBoolean(KEY_HAS_RATED, true)
        store.putBoolean(KEY_REVIEW_FLOW_COMPLETED, true)
        store.putLong(KEY_LAST_PROMPT_AT_MILLIS, nowMillis)
        store.putInt(
            KEY_PROMPT_ATTEMPT_COUNT,
            (store.getInt(KEY_PROMPT_ATTEMPT_COUNT) ?: 0) + 1
        )
    }

    fun snoozeReviewPrompt(durationMillis: Long, nowMillis: Long) {
        store.putLong(KEY_SNOOZED_UNTIL, nowMillis + durationMillis)
        store.putLong(KEY_LAST_PROMPT_AT_MILLIS, nowMillis)
    }

    private fun ensureFirstOpenedAtMillis(nowMillis: Long): Long {
        val existingValue = store.getLong(KEY_FIRST_LAUNCH_TIME) ?: 0L
        if (existingValue > 0L) return existingValue

        store.putLong(KEY_FIRST_LAUNCH_TIME, nowMillis)
        return nowMillis
    }

    companion object {
        const val PREFS_NAME = "app_rating"
        const val MIN_LAUNCH_COUNT = 5
        const val MIN_MEANINGFUL_INTERACTIONS = 15
        const val MIN_USAGE_AGE_MILLIS = 3L * 24L * 60L * 60L * 1000L
        const val SHORT_RETRY_DELAY_MILLIS = 7L * 24L * 60L * 60L * 1000L
        const val USER_DISMISS_DELAY_MILLIS = 30L * 24L * 60L * 60L * 1000L

        private const val KEY_FIRST_LAUNCH_TIME = "first_launch_time"
        private const val KEY_HAS_RATED = "has_rated"
        private const val KEY_LAUNCH_COUNT = "launch_count"
        private const val KEY_SCREEN_VISIT_COUNT = "screen_visit_count"
        private const val KEY_SNOOZED_UNTIL = "snoozed_until"
        private const val KEY_REVIEW_FLOW_COMPLETED = "review_flow_completed"
        private const val KEY_LAST_PROMPT_AT_MILLIS = "last_prompt_at_millis"
        private const val KEY_PROMPT_ATTEMPT_COUNT = "prompt_attempt_count"
    }
}
