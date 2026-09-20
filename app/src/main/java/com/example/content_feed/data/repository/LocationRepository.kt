package com.example.content_feed.data.repository

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            // 1. Try to get cached last known location first (almost instantaneous)
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return lastLocation
            }

            // 2. If no cache, request current location with balanced power accuracy and 5s timeout
            withTimeoutOrNull(5000L) {
                val cts = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).await()
            }
        } catch (e: Exception) {
            null
        }
    }
}
