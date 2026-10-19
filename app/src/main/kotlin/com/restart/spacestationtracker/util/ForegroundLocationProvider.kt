package com.restart.spacestationtracker.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object ForegroundLocationProvider {
    private const val FUSED_LAST_LOCATION_TIMEOUT_MS = 1_500L
    private const val FRESH_LOCATION_TIMEOUT_MS = 10_000L
    private const val MAX_CACHED_LOCATION_AGE_MS = 30L * 60L * 1_000L
    private const val MAX_CACHED_ACCURACY_METERS = 5_000f

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Prefers a recent cached fix, then a fresh fix, and only falls back to an old cached fix
     * when nothing else is available, so a days-old location from another city is a last resort.
     */
    suspend fun getBestLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        val lastKnownLocation = getLocationManagerLastKnownLocation(context)
        val fusedLastLocation = getFusedLastLocation(context)
        return listOfNotNull(lastKnownLocation, fusedLastLocation)
            .filter(::isRecent)
            .maxByOrNull(Location::getTime)
            ?: getFreshFusedLocation(context)
            ?: listOfNotNull(lastKnownLocation, fusedLastLocation).maxByOrNull(Location::getTime)
    }

    private fun isRecent(location: Location): Boolean {
        val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        val isAccurateEnough = !location.hasAccuracy() || location.accuracy <= MAX_CACHED_ACCURACY_METERS
        return ageMillis in 0..MAX_CACHED_LOCATION_AGE_MS && isAccurateEnough
    }

    private fun getLocationManagerLastKnownLocation(context: Context): Location? {
        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.getProviders(true)
                .mapNotNull { provider ->
                    try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
                .maxByOrNull(Location::getTime)
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private suspend fun getFusedLastLocation(context: Context): Location? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            withTimeoutOrNull(FUSED_LAST_LOCATION_TIMEOUT_MS) {
                fusedLocationClient.lastLocation.awaitOrNull()
            }
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private suspend fun getFreshFusedLocation(context: Context): Location? {
        val cancellationTokenSource = CancellationTokenSource()
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            withTimeoutOrNull(FRESH_LOCATION_TIMEOUT_MS) {
                fusedLocationClient
                    .getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.token
                    )
                    .awaitOrNull()
            }
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        } finally {
            cancellationTokenSource.cancel()
        }
    }
}

private suspend fun <T> Task<T>.awaitOrNull(): T? {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            continuation.resume(if (task.isSuccessful) task.result else null)
        }
    }
}
