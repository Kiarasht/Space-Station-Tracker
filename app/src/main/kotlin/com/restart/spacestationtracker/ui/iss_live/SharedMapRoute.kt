package com.restart.spacestationtracker.ui.iss_live

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.restart.spacestationtracker.analytics.AppAnalytics
import com.restart.spacestationtracker.shared.ui.SharedIssMapScreen
import com.restart.spacestationtracker.shared.ui.SharedRemoveAdsButton
import com.restart.spacestationtracker.ui.purchase.AdRemovalPurchaseUiState

@Composable
fun SharedMapRoute(
    contentPadding: PaddingValues,
    canRequestAds: Boolean,
    purchaseState: AdRemovalPurchaseUiState,
    onPurchaseAdRemoval: () -> Unit,
    onRestoreAdRemoval: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Poll only while the map destination is started: not on other tabs or in the background.
    LifecycleStartEffect(viewModel) {
        viewModel.startIssTracking()
        onStopOrDispose { viewModel.stopIssTracking() }
    }

    SharedIssMapScreen(
        currentLocation = state.issLocation,
        futureLocations = state.futureIssLocations,
        mapType = state.mapType,
        units = state.units,
        isLoading = state.isLoading,
        error = state.error,
        liveStreams = state.liveStreams,
        contentPadding = contentPadding,
        satellites = state.satellites,
        onOpenStream = { stream ->
            AppAnalytics.trackInteraction("open_live_stream", "map")
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://www.youtube.com/watch?v=${stream.videoId}".toUri()
                )
            )
        },
        topEndContent = {
            if (!state.isAdFree && canRequestAds) {
                SharedRemoveAdsButton(
                    purchasePriceText = purchaseState.priceText,
                    isPurchaseInProgress = purchaseState.isPurchaseInProgress,
                    isPurchaseAvailable = purchaseState.isPurchaseAvailable &&
                        !purchaseState.isLoading,
                    purchaseStatusCode = purchaseState.statusCode,
                    onOpen = {
                        AppAnalytics.trackInteraction("open_ad_removal", "map")
                    },
                    onPurchase = onPurchaseAdRemoval,
                    onRestore = onRestoreAdRemoval
                )
            }
        }
    )
}
