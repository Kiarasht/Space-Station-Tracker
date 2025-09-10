package com.restart.spacestationtracker.ui.people_in_space

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.people_in_space.use_case.GetPeopleInSpaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class PeopleInSpaceViewModel @Inject constructor(
    private val getPeopleInSpaceUseCase: GetPeopleInSpaceUseCase,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleInSpaceUiState())
    val uiState: StateFlow<PeopleInSpaceUiState> = _uiState.asStateFlow()

    init {
        loadPeopleInSpace()
    }

    private fun loadPeopleInSpace() {
        viewModelScope.launch {
            _uiState.value = PeopleInSpaceUiState(isLoading = true)
            getPeopleInSpaceUseCase().onSuccess { (expedition, astronauts) ->
                val feedItems: MutableList<FeedItem> = mutableListOf()
                feedItems.add(FeedItem.ExpeditionItem(expedition))

                var astronautIndex = 0
                while (astronautIndex < astronauts.size) {
                    feedItems.add(FeedItem.AstronautItem(astronauts[astronautIndex]))
                    astronautIndex++
                    if (astronautIndex == 2) {
                        loadNativeAd()?.let { feedItems.add(FeedItem.AdItem(it)) }
                    } else if (astronautIndex > 2 && (astronautIndex - 2) % 3 == 0) {
                        loadNativeAd()?.let { feedItems.add(FeedItem.AdItem(it)) }
                    }
                }
                _uiState.value = PeopleInSpaceUiState(feedItems = feedItems)
            }.onFailure { throwable ->
                _uiState.value = PeopleInSpaceUiState(error = throwable.localizedMessage ?: "An unknown error occurred")
            }
        }
    }

    private suspend fun loadNativeAd(): NativeAd? {
        val adUnitId = application.getString(R.string.on_duty_native_ad_unit_id)
        return suspendCoroutine { continuation ->
            val adLoader = AdLoader.Builder(application, adUnitId)
                .forNativeAd { nativeAd ->
                    continuation.resume(nativeAd)
                }
                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                        continuation.resume(null)
                    }
                })
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }
}
