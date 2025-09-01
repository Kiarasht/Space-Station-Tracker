package com.restart.spacestationtracker.ui.iss_passes

import com.google.android.gms.ads.nativead.NativeAd
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.iss_passes.use_case.UserLocation

sealed interface FeedItem {
    data class PassItem(val pass: IssPass) : FeedItem
    data class AdItem(val ad: NativeAd) : FeedItem
}

data class IssPassesUiState(
    val isLoading: Boolean = false,
    val feedItems: List<FeedItem> = emptyList(),
    val error: String? = null,
    val permissionGranted: Boolean = false,
    val location: UserLocation? = null
)