package com.restart.spacestationtracker.ui.people_in_space

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.domain.people_in_space.use_case.GetPeopleInSpaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class PeopleInSpaceViewModel @Inject constructor(
    private val getPeopleInSpaceUseCase: GetPeopleInSpaceUseCase,
    private val settingsRepository: SettingsRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleInSpaceUiState())
    val uiState: StateFlow<PeopleInSpaceUiState> = _uiState.asStateFlow()

    private var rawData: Pair<Expedition, List<Astronaut>>? = null

    init {
        loadPeopleInSpace()
        observeAdFreeStatus()
    }

    private fun loadPeopleInSpace() {
        viewModelScope.launch {
            _uiState.value = PeopleInSpaceUiState(isLoading = true)
            getPeopleInSpaceUseCase().onSuccess { (expedition, astronauts) ->
                rawData = expedition to astronauts
                val settings = settingsRepository.appSettingsFlow.first()
                val isAdFree = System.currentTimeMillis() < settings.adFreeExpiry
                buildFeed(expedition, astronauts, isAdFree)
            }.onFailure { throwable ->
                _uiState.value = PeopleInSpaceUiState(error = throwable.localizedMessage ?: "An unknown error occurred")
            }
        }
    }

    private fun observeAdFreeStatus() {
        viewModelScope.launch {
            settingsRepository.appSettingsFlow
                .map { System.currentTimeMillis() < it.adFreeExpiry }
                .distinctUntilChanged()
                .collect { isAdFree ->
                    rawData?.let { (expedition, astronauts) ->
                        buildFeed(expedition, astronauts, isAdFree)
                    }
                }
        }
    }

    private suspend fun buildFeed(expedition: Expedition, astronauts: List<Astronaut>, isAdFree: Boolean) {
        val feedItems: MutableList<FeedItem> = mutableListOf()
        feedItems.add(FeedItem.ExpeditionItem(expedition))

        var astronautIndex = 0
        while (astronautIndex < astronauts.size) {
            feedItems.add(FeedItem.AstronautItem(astronauts[astronautIndex]))
            astronautIndex++

            if (!isAdFree) {
                if (astronautIndex == 2) {
                    loadNativeAd()?.let { feedItems.add(FeedItem.AdItem(it)) }
                } else if (astronautIndex > 2 && (astronautIndex - 2) % 3 == 0) {
                    loadNativeAd()?.let { feedItems.add(FeedItem.AdItem(it)) }
                }
            }
        }
        _uiState.value = PeopleInSpaceUiState(feedItems = feedItems)
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
