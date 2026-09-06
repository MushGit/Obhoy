package com.obhoy.app.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class GnssSatelliteEngine(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @Volatile
    var lastKnownLocation: Location? = null
        private set

    /**
     * Attempts a fresh live GNSS satellite fix (up to 45s).
     * If offline/blocked, falls back to the system cache.
     * If system cache is wiped, falls back to the last online location saved in Obhoy DB.
     */
    @SuppressLint("MissingPermission")
    suspend fun awaitFreshLocation(
        timeoutMs: Long = 45_000L,
        dbFallbackLocation: Location? = null // Pass last saved location from Room DB
    ): Location? = withContext(Dispatchers.IO) {
        val cancellationTokenSource = CancellationTokenSource()

        // 1. Try to get live offline satellite fix
        val freshLocation = withTimeoutOrNull(timeoutMs) {
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            } catch (e: Exception) {
                null
            }
        }

        if (isValidLocation(freshLocation)) {
            saveLocationToLocalDb(freshLocation!!)
            lastKnownLocation = freshLocation
            return@withContext freshLocation
        }

        // 2. Fallback to System Fused Cache
        val cachedSystemLocation = try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }

        if (isValidLocation(cachedSystemLocation)) {
            lastKnownLocation = cachedSystemLocation
            return@withContext cachedSystemLocation
        }

        // 3. Ultimate Fallback: Last known online location from Obhoy's local DB
        val finalFallback = if (isValidLocation(dbFallbackLocation)) dbFallbackLocation else null
        lastKnownLocation = finalFallback
        return@withContext finalFallback
    }

    /**
     * Call this whenever your app receives a valid location while online
     * to keep your offline fallback fresh.
     */
    private fun saveLocationToLocalDb(location: Location) {
        // e.g., obhoyDatabase.locationDao().insert(LocationEntity(location.latitude, location.longitude, System.currentTimeMillis()))
    }

    private fun isValidLocation(location: Location?): Boolean {
        if (location == null) return false
        if (location.latitude == 0.0 && location.longitude == 0.0) return false
        return true
    }
}
