package com.restart.spacestationtracker.ui.iss_passes

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.iss_passes.use_case.GetIssPassesUseCase
import com.restart.spacestationtracker.domain.iss_passes.use_case.UserLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class IssPassesViewModel @Inject constructor(
    private val getIssPassesUseCase: GetIssPassesUseCase,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssPassesUiState())
    val uiState: StateFlow<IssPassesUiState> = _uiState.asStateFlow()

    init {
        checkPermission()
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = isGranted)
        if (isGranted) {
            fetchLocationAndPasses()
        } else {
            _uiState.value =
                _uiState.value.copy(error = "Location permission is required to show ISS passes.")
        }
    }

    private fun checkPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.value = _uiState.value.copy(permissionGranted = hasPermission)
        if (hasPermission) {
            fetchLocationAndPasses()
        }
    }

    private fun fetchLocationAndPasses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val locationManager =
                    application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location =
                    locationManager.getProviders(true).asReversed().firstNotNullOfOrNull { provider ->
                        try {
                            locationManager.getLastKnownLocation(provider)
                        } catch (_: SecurityException) {
                            null
                        }
                    }

                if (location != null) {
                    val geocoder = Geocoder(application)
                    val address = getAddressFromLocation(geocoder, location)

                    val locationName =
                        address?.let { "${it.locality}, ${it.adminArea}" } ?: "Current Location"
                    val userLocation = UserLocation(
                        location.latitude,
                        location.longitude,
                        location.altitude,
                        locationName
                    )

                    _uiState.value = _uiState.value.copy(location = userLocation)

                    getIssPassesUseCase(userLocation).onSuccess { passes ->
                        val feedItems: MutableList<FeedItem> =
                            passes.map { FeedItem.PassItem(it) }.toMutableList()
                        val adPosition = 2
                        val adInterval = 4
                        var insertionIndex = adPosition

                        while (insertionIndex <= feedItems.size) {
                            loadNativeAd()?.let { adItem ->
                                feedItems.add(insertionIndex, FeedItem.AdItem(adItem))
                            }
                            insertionIndex += adInterval
                        }
                        _uiState.value = _uiState.value.copy(
                            feedItems = feedItems,
                            isLoading = false,
                            error = null
                        )
                    }.onFailure {
                        _uiState.value =
                            _uiState.value.copy(isLoading = false, error = it.localizedMessage)
                    }
                } else {
                    _uiState.value =
                        _uiState.value.copy(isLoading = false, error = "Could not retrieve location.")
                }
            } catch (_: SecurityException) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Location permission denied.")
            }
        }
    }

    private suspend fun getAddressFromLocation(geocoder: Geocoder, location: Location): Address? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    ) { addresses ->
                        continuation.resume(addresses.firstOrNull())
                    }
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                }
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            } catch (_: IOException) {
                null
            }
        }
    }


    private suspend fun loadNativeAd(): NativeAd? {
        val adUnitId = application.getString(R.string.locations_native_ad_unit_id)
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