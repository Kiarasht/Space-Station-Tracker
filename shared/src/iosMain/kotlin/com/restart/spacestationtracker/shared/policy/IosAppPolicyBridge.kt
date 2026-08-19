package com.restart.spacestationtracker.shared.policy

import com.restart.spacestationtracker.shared.preferences.NSUserDefaultsPreferenceStore

class IosAppPolicyBridge(
    appOpenSuiteName: String = AppOpenRepository.PREFS_NAME,
    reviewSuiteName: String = AppReviewRepository.PREFS_NAME
) {
    private val appOpenRepository = AppOpenRepository(
        NSUserDefaultsPreferenceStore(appOpenSuiteName)
    )
    private val reviewRepository = AppReviewRepository(
        NSUserDefaultsPreferenceStore(reviewSuiteName)
    )

    fun appOpenStartThreshold(): Int = MonetizationPolicy.APP_OPEN_START_THRESHOLD

    fun appOpenExpirationMillis(): Long = MonetizationPolicy.APP_OPEN_EXPIRATION_MILLIS

    fun recordAppOpen(): Int = appOpenRepository.recordAppOpen()

    fun recordReviewAppOpen(nowMillis: Long) {
        reviewRepository.recordAppOpened(nowMillis)
    }

    fun recordMeaningfulInteraction(nowMillis: Long) {
        reviewRepository.recordMeaningfulInteraction(nowMillis)
    }

    fun isReviewPromptEligible(nowMillis: Long): Boolean {
        return reviewRepository.isEligibleForReviewPrompt(nowMillis)
    }

    fun markReviewFlowCompleted(nowMillis: Long) {
        reviewRepository.markReviewFlowCompleted(nowMillis)
    }

    fun snoozeReviewPrompt(durationMillis: Long, nowMillis: Long) {
        reviewRepository.snoozeReviewPrompt(durationMillis, nowMillis)
    }
}
