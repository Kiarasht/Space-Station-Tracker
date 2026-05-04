package com.restart.spacestationtracker.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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

    suspend fun getBestLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        return getLocationManagerLastKnownLocation(context)
            ?: getFusedLastLocation(context)
            ?: getFreshFusedLocation(context)
            ?: getLocationManagerLastKnownLocation(context)
    }

    private fun getLocationManagerLastKnownLocation(context: Context): Location? {
        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.getProviders(true)
                .asReversed()
                .firstNotNullOfOrNull { provider ->
                    try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
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
