package com.restart.spacestationtracker.ui.people_in_space

import com.google.android.gms.ads.nativead.NativeAd
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut

sealed interface FeedItem {
    data class AstronautItem(val astronaut: Astronaut) : FeedItem
    data class AdItem(val ad: NativeAd) : FeedItem
}

data class PeopleInSpaceUiState(
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
