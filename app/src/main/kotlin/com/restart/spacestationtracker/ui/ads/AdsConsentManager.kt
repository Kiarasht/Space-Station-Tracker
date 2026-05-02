package com.restart.spacestationtracker.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsConsentManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    fun gatherConsent(activity: Activity, onConsentGatheringComplete: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                updateConsentState()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    updateConsentState()
                    onConsentGatheringComplete(consentInformation.canRequestAds())
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                updateConsentState()
                onConsentGatheringComplete(consentInformation.canRequestAds())
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (() -> Unit)? = null) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.message}")
            }
            updateConsentState()
            onDismissed?.invoke()
        }
    }

    fun refreshConsentState() {
        updateConsentState()
    }

    private fun updateConsentState() {
        _canRequestAds.value = consentInformation.canRequestAds()
        _isPrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private companion object {
        const val TAG = "AdsConsentManager"
    }
}
