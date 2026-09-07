package com.obhoy.app.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.GnssStatus
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class GnssSatelliteEngine(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Volatile
    var lastKnownLocation: Location? = null
        private set

    // Real satellite telemetry from the GPS chip — updated live via GnssStatus.Callback
    @Volatile
    var satellitesInView: Int = 0
        private set

    @Volatile
    var satellitesUsedInFix: Int = 0
        private set

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            satellitesInView = status.satelliteCount
            var usedCount = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) usedCount++
            }
            satellitesUsedInFix = usedCount
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return
        locationManager.registerGnssStatusCallback(gnssStatusCallback, null)
    }

    fun stopListening() {
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
    }

    /**
     * Requests a fresh fix directly from the GPS chip via LocationManager,
     * bypassing Google Play Services / Fused Location entirely. This works
     * with zero network connectivity and on devices without GMS installed.
     * Falls back to the provider's last-known fix, then an optional caller-
     * supplied DB fallback, if a live fix can't be obtained in time.
     */
    @SuppressLint("MissingPermission")
    suspend fun awaitFreshLocation(
        timeoutMs: Long = 45_000L,
        dbFallbackLocation: Location? = null
    ): Location? = withContext(Dispatchers.IO) {

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return@withContext resolveFallback(dbFallbackLocation)
        }

        val freshLocation = withTimeoutOrNull(timeoutMs) {
            requestSingleGpsUpdate()
        }

        if (isValidLocation(freshLocation)) {
            lastKnownLocation = freshLocation
            return@withContext freshLocation
        }

        // Fallback 1: provider's own last-known fix (may be stale, but instant)
        val cached = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            null
        }

        if (isValidLocation(cached)) {
            lastKnownLocation = cached
            return@withContext cached
        }

        return@withContext resolveFallback(dbFallbackLocation)
    }

    private fun resolveFallback(dbFallbackLocation: Location?): Location? {
        val fallback = if (isValidLocation(dbFallbackLocation)) dbFallbackLocation else null
        lastKnownLocation = fallback
        return fallback
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleGpsUpdate(): Location? =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (continuation.isActive) continuation.resume(location)
                    locationManager.removeUpdates(this)
                }

                @Deprecated("Deprecated in API 29+, required for older API compatibility")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }

            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }
        }

    private fun isValidLocation(location: Location?): Boolean {
        if (location == null) return false
        if (location.latitude == 0.0 && location.longitude == 0.0) return false
        return true
    }
}
