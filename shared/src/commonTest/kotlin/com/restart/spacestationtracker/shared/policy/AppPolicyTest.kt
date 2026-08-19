package com.restart.spacestationtracker.shared.policy

import com.restart.spacestationtracker.shared.preferences.PreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPolicyTest {
    @Test
    fun appOpenCounterPreservesTheExistingAndroidStorageContract() {
        val store = InMemoryPreferenceStore()
        store.putInt(AppOpenRepository.KEY_FOREGROUND_OPEN_COUNT, 4)

        val repository = AppOpenRepository(store)

        assertEquals(5, repository.recordAppOpen())
        assertEquals(AppOpenRepository.PREFS_NAME, "app_open_ads")
    }

    @Test
    fun reviewPromptRequiresAgeLaunchesAndMeaningfulInteractions() {
        val store = InMemoryPreferenceStore()
        val repository = AppReviewRepository(store)
        val firstOpen = 1_000L

        repeat(AppReviewRepository.MIN_LAUNCH_COUNT) {
            repository.recordAppOpened(firstOpen)
        }
        repeat(AppReviewRepository.MIN_MEANINGFUL_INTERACTIONS) {
            repository.recordMeaningfulInteraction(firstOpen)
        }

        assertFalse(
            repository.isEligibleForReviewPrompt(
                firstOpen + AppReviewRepository.MIN_USAGE_AGE_MILLIS - 1
            )
        )
        assertTrue(
            repository.isEligibleForReviewPrompt(
                firstOpen + AppReviewRepository.MIN_USAGE_AGE_MILLIS
            )
        )
    }

    @Test
    fun completedReviewFlowIsNotRequestedAgain() {
        val store = InMemoryPreferenceStore()
        val repository = AppReviewRepository(store)
        val firstOpen = 1_000L
        val eligibleTime = firstOpen + AppReviewRepository.MIN_USAGE_AGE_MILLIS

        repeat(AppReviewRepository.MIN_LAUNCH_COUNT) {
            repository.recordAppOpened(firstOpen)
        }
        repeat(AppReviewRepository.MIN_MEANINGFUL_INTERACTIONS) {
            repository.recordMeaningfulInteraction(firstOpen)
        }
        repository.markReviewFlowCompleted(eligibleTime)

        assertFalse(repository.isEligibleForReviewPrompt(eligibleTime))
    }

    @Test
    fun existingAndroidRatedFlagIsPreserved() {
        val store = InMemoryPreferenceStore()
        store.putBoolean("has_rated", true)
        val repository = AppReviewRepository(store)

        assertFalse(
            repository.isEligibleForReviewPrompt(
                AppReviewRepository.MIN_USAGE_AGE_MILLIS
            )
        )
    }
}

private class InMemoryPreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, Any>()

    override fun getInt(key: String): Int? = values[key] as? Int

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getLong(key: String): Long? = values[key] as? Long

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getBoolean(key: String): Boolean? = values[key] as? Boolean

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}
