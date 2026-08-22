package com.restart.spacestationtracker.ui.people_in_space

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.restart.spacestationtracker.analytics.AppAnalytics
import com.restart.spacestationtracker.shared.ui.SharedCrewItem
import com.restart.spacestationtracker.shared.ui.SharedCrewScreen
import com.restart.spacestationtracker.ui.ads.NativeAdCard

@Composable
fun PeopleInSpaceScreen(
    viewModel: PeopleInSpaceViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val uiState by viewModel.uiState.collectAsState()
    val screenPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateEndPadding(layoutDirection),
        bottom = contentPadding.calculateTopPadding()
    )
    val ads = remember(uiState.feedItems) {
        uiState.feedItems.filterIsInstance<FeedItem.AdItem>()
            .associate { "ad-${it.ad.hashCode()}" to it.ad }
    }
    val sharedItems = remember(uiState.feedItems) {
        uiState.feedItems.map { item ->
            when (item) {
                is FeedItem.ExpeditionItem -> SharedCrewItem.ExpeditionItem(item.expedition)
                is FeedItem.AstronautItem -> SharedCrewItem.AstronautItem(item.astronaut)
                is FeedItem.AdItem -> SharedCrewItem.PlatformSlot("ad-${item.ad.hashCode()}")
            }
        }
    }

    SharedCrewScreen(
        items = sharedItems,
        isLoading = uiState.isLoading,
        error = uiState.error,
        contentPadding = screenPadding,
        onRetry = {
            AppAnalytics.trackInteraction("retry_crew", "on_duty")
            viewModel.retry()
        },
        onOpenUrl = { url ->
            AppAnalytics.trackInteraction("open_crew_external_content", "on_duty")
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        },
        platformSlot = { id -> ads[id]?.let { NativeAdCard(nativeAd = it) } }
    )
}
