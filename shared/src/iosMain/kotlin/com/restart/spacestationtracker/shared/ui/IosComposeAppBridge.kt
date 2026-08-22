package com.restart.spacestationtracker.shared.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.restart.spacestationtracker.shared.presentation.SharedAppController
import com.restart.spacestationtracker.shared.preferences.NSUserDefaultsPreferenceStore
import com.restart.spacestationtracker.shared.settings.SharedSettingsRepository
import com.restart.spacestationtracker.shared.ui.platform.IosIssMapRegistry
import com.restart.spacestationtracker.shared.ui.platform.IosBannerAdRegistry
import com.restart.spacestationtracker.shared.ui.platform.IosNativeAdRegistry
import com.restart.spacestationtracker.shared.ui.platform.IssMapPoint
import kotlinx.coroutines.flow.MutableStateFlow
import platform.UIKit.UIView
import platform.UIKit.UIViewController

class IosComposeAppBridge(
    private val versionText: String = "Version 7.09 (53)"
) {
    private val controller = SharedAppController(
        settingsRepository = SharedSettingsRepository(
            NSUserDefaultsPreferenceStore(SETTINGS_SUITE_NAME)
        )
    )
    private var actionHandler: ((String, String?) -> Unit)? = null
    private val settingsPlatformState = MutableStateFlow(SharedSettingsPlatformState())

    @OptIn(ExperimentalComposeUiApi::class)
    fun createViewController(): UIViewController {
        return ComposeUIViewController(configure = {
            opaque = true
        }) {
            val platformState by settingsPlatformState.collectAsState()
            SharedAppRoot(
                controller = controller,
                versionText = versionText,
                settingsPlatformState = platformState,
                onAction = { actionId, parameter ->
                    actionHandler?.invoke(actionId, parameter)
                }
            )
        }
    }

    fun setActionHandler(handler: (String, String?) -> Unit) {
        actionHandler = handler
    }

    fun registerMapViewFactory(
        factory: () -> UIView,
        updater: (UIView, IssMapPoint?, List<IssMapPoint>, String) -> Unit
    ) {
        IosIssMapRegistry.register(factory, updater)
    }

    fun registerNativeAdViewFactory(factory: (String) -> UIView) {
        IosNativeAdRegistry.register(factory)
    }

    fun registerBannerAdViewFactory(factory: () -> UIView) {
        IosBannerAdRegistry.register(factory)
    }

    fun updateLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        locationName: String
    ) {
        controller.setAutomaticPassAlertLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            locationName = locationName
        )
        controller.loadPasses(latitude, longitude, altitude, locationName)
    }

    fun setAutomaticPassAlertsEnabled(enabled: Boolean) {
        controller.setAutomaticPassAlertsEnabled(enabled)
    }

    fun clearPassesError() {
        controller.clearPassesError()
    }

    fun setPassesError(message: String) {
        controller.setPassesError(message)
    }

    fun setPassesLocationError(message: String) {
        controller.setPassesLocationError(message)
    }

    fun updateSettingsPlatformState(
        hasNotificationPermission: Boolean,
        hasLocationPermission: Boolean,
        isBackgroundUnrestricted: Boolean,
        isLocationLookupInProgress: Boolean,
        showPrivacyChoices: Boolean,
        adsAvailable: Boolean,
        isAdFree: Boolean,
        purchasePriceText: String,
        isPurchaseInProgress: Boolean,
        isPurchaseAvailable: Boolean,
        purchaseStatusCode: String?
    ) {
        settingsPlatformState.value = SharedSettingsPlatformState(
            hasNotificationPermission = hasNotificationPermission,
            hasLocationPermission = hasLocationPermission,
            isBackgroundUnrestricted = isBackgroundUnrestricted,
            isLocationLookupInProgress = isLocationLookupInProgress,
            showPrivacyChoices = showPrivacyChoices,
            adsAvailable = adsAvailable,
            isAdFree = isAdFree,
            purchasePriceText = purchasePriceText,
            isPurchaseInProgress = isPurchaseInProgress,
            isPurchaseAvailable = isPurchaseAvailable,
            purchaseStatusCode = purchaseStatusCode
        )
    }

    private companion object {
        const val SETTINGS_SUITE_NAME = "settings"
    }
}
