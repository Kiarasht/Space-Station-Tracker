package com.restart.spacestationtracker.ui.iss_passes

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.iss_passes.use_case.GetIssPassesUseCase
import com.restart.spacestationtracker.domain.iss_passes.use_case.UserLocation
import com.restart.spacestationtracker.ui.ads.AdMobIds
import com.restart.spacestationtracker.ui.ads.AdsConsentManager
import com.restart.spacestationtracker.util.ForegroundLocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class IssPassesViewModel @Inject constructor(
    private val getIssPassesUseCase: GetIssPassesUseCase,
    private val settingsRepository: SettingsRepository,
    private val adsConsentManager: AdsConsentManager,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssPassesUiState())
    val uiState: StateFlow<IssPassesUiState> = _uiState.asStateFlow()

    private var rawPasses: List<IssPass>? = null
    private var fetchJob: Job? = null
    private var feedJob: Job? = null
    private var loadedAds: List<NativeAd> = emptyList()

    init {
        checkPermission()
        observeAdFreeStatus()
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = isGranted)
        if (isGranted) {
            fetchLocationAndPasses()
        } else {
            showLocationError(R.string.sky_path_location_permission_required)
        }
    }

    fun retryLocationAndPasses() {
        if (ForegroundLocationProvider.hasLocationPermission(application)) {
            _uiState.value = _uiState.value.copy(
                permissionGranted = true,
                error = null,
                isLocationError = false
            )
            fetchLocationAndPasses()
        } else {
            _uiState.value = _uiState.value.copy(permissionGranted = false)
            showLocationError(R.string.sky_path_location_permission_required)
        }
    }

    private fun checkPermission() {
        val hasPermission = ForegroundLocationProvider.hasLocationPermission(application)
        _uiState.value = _uiState.value.copy(permissionGranted = hasPermission)
        if (hasPermission) {
            fetchLocationAndPasses()
        }
    }

    private fun observeAdFreeStatus() {
        viewModelScope.launch {
            settingsRepository.appSettingsFlow
                .map { it.hasLifetimeAdRemoval }
                .combine(adsConsentManager.canRequestAds) { isAdFree, canRequestAds ->
                    isAdFree to canRequestAds
                }
                .distinctUntilChanged()
                .collect { (isAdFree, canRequestAds) ->
                    rawPasses?.let { passes ->
                        buildFeed(passes, isAdFree, canRequestAds)
                    }
                }
        }
    }

    private fun fetchLocationAndPasses() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val location = ForegroundLocationProvider.getBestLocation(application)

                if (location != null) {
                    val address = getAddressFromLocation(Geocoder(application), location)

                    val locationName =
                        address?.toDisplayName()
                            ?: application.getString(R.string.current_location)
                    val userLocation = UserLocation(
                        location.latitude,
                        location.longitude,
                        location.altitude,
                        locationName
                    )

                    _uiState.value = _uiState.value.copy(location = userLocation)

                    getIssPassesUseCase(userLocation).onSuccess { passes ->
                        rawPasses = passes
                        val settings = settingsRepository.appSettingsFlow.first()
                        val isAdFree = settings.hasLifetimeAdRemoval
                        buildFeed(passes, isAdFree, adsConsentManager.canRequestAds.value)
                    }.onFailure {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLocationError = false,
                            error = application.getString(R.string.unable_to_get_passes)
                        )
                    }
                } else {
                    showLocationError(R.string.could_not_retrieve_location)
                }
            } catch (_: SecurityException) {
                showLocationError(R.string.location_permission_denied)
            }
        }
    }

    private fun showLocationError(messageResId: Int) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isLocationError = true,
            error = application.getString(messageResId)
        )
    }

    // Passes are shown right away; native ads are loaded in parallel and slotted in afterwards.
    private fun buildFeed(passes: List<IssPass>, isAdFree: Boolean, canRequestAds: Boolean) {
        feedJob?.cancel()
        _uiState.value = _uiState.value.copy(
            feedItems = passes.map { FeedItem.PassItem(it) },
            isLoading = false,
            isLocationError = false,
            error = null
        )
        replaceLoadedAds(emptyList())
        if (isAdFree || !canRequestAds) return

        var adSlots = 0
        while (AD_FIRST_POSITION + adSlots * AD_INTERVAL <= passes.size + adSlots) {
            adSlots++
        }
        if (adSlots == 0) return

        feedJob = viewModelScope.launch {
            val ads = coroutineScope {
                List(adSlots) { async { loadNativeAd() } }.awaitAll().filterNotNull()
            }
            replaceLoadedAds(ads)

            val feedItems: MutableList<FeedItem> =
                passes.map { FeedItem.PassItem(it) }.toMutableList()
            var insertionIndex = AD_FIRST_POSITION
            ads.forEach { ad ->
                if (insertionIndex > feedItems.size) return@forEach
                feedItems.add(insertionIndex, FeedItem.AdItem(ad))
                insertionIndex += AD_INTERVAL
            }
            _uiState.value = _uiState.value.copy(feedItems = feedItems)
        }
    }

    private fun replaceLoadedAds(ads: List<NativeAd>) {
        loadedAds.forEach(NativeAd::destroy)
        loadedAds = ads
    }

    // A missing place name must never block the pass lookup, so every failure path yields null.
    private suspend fun getAddressFromLocation(geocoder: Geocoder, location: Location): Address? {
        if (!Geocoder.isPresent()) return null
        return withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (continuation.isActive) {
                                        continuation.resume(addresses.firstOrNull())
                                    }
                                }

                                override fun onError(errorMessage: String?) {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                            }
                        )
                    } catch (_: IllegalArgumentException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            ?.firstOrNull()
                    } catch (_: IOException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            }
        }
    }

    private fun Address.toDisplayName(): String? {
        val nearbyPlace = sequenceOf(locality, subLocality, subAdminArea, featureName)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
        val region = sequenceOf(adminArea, countryName)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()

        return when {
            nearbyPlace != null && region != null &&
                !nearbyPlace.equals(region, ignoreCase = true) -> "$nearbyPlace, $region"
            nearbyPlace != null -> nearbyPlace
            else -> region
        }
    }


    private suspend fun loadNativeAd(): NativeAd? {
        val adUnitId = AdMobIds.nativeForPasses(application)
        return suspendCancellableCoroutine { continuation ->
            val adLoader = AdLoader.Builder(application, adUnitId)
                .forNativeAd { nativeAd ->
                    if (continuation.isActive) {
                        continuation.resume(nativeAd)
                    } else {
                        nativeAd.destroy()
                    }
                }
                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                })
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    override fun onCleared() {
        replaceLoadedAds(emptyList())
        super.onCleared()
    }

    private companion object {
        const val GEOCODER_TIMEOUT_MS = 5_000L
        const val AD_FIRST_POSITION = 2
        const val AD_INTERVAL = 4
    }
}
