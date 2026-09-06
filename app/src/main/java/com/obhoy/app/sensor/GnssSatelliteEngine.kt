package com.obhoy.app.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class GnssSatelliteEngine(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Volatile
    var lastKnownLocation: Location? = null
        private set

    /**
     * Legacy background listener lifecycle (retained for continuous service tracking).
     */
    @SuppressLint("MissingPermission")
    fun startListening() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                1f,
                this,
                Looper.getMainLooper()
            )
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                2000L,
                1f,
                this,
                Looper.getMainLooper()
            )
        }

        lastKnownLocation = getCachedFallbackLocation()
    }

    fun stopListening() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        if (isBetterLocation(location, lastKnownLocation)) {
            lastKnownLocation = location
        }
    }

    /**
     * Suspends execution during an emergency dispatch to guarantee a fresh live GNSS satellite fix.
     * Waits up to [timeoutMs] (default 60 seconds for cold offline fixes).
     * Falls back to cached position if satellites cannot lock in time.
     */
    @SuppressLint("MissingPermission")
    suspend fun awaitFreshLocation(timeoutMs: Long = 60_000L): Location? = withContext(Dispatchers.Main) {
        val cachedFallback = getCachedFallbackLocation()

        // Wait up to 60 seconds for a physical GNSS satellite lock
        val freshLocation = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val singleUpdateListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}

                    @Deprecated("Deprecated in API 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        0f,
                        singleUpdateListener,
                        Looper.getMainLooper()
                    )
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        0f,
                        singleUpdateListener,
                        Looper.getMainLooper()
                    )
                } else {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(singleUpdateListener)
                }
            }
        }

        val finalBestLocation = freshLocation ?: cachedFallback
        lastKnownLocation = finalBestLocation
        return@withContext finalBestLocation
    }

    @SuppressLint("MissingPermission")
    private fun getCachedFallbackLocation(): Location? {
        val gpsLocation = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else null

        val netLocation = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } else null

        return when {
            gpsLocation != null && netLocation != null -> {
                if (gpsLocation.time >= netLocation.time) gpsLocation else netLocation
            }
            gpsLocation != null -> gpsLocation
            else -> netLocation
        }
    }

    private fun isBetterLocation(location: Location, currentBest: Location?): Boolean {
        if (currentBest == null) return true
        val timeDelta: Long = location.time - currentBest.time
        val isSignificantlyNewer: Boolean = timeDelta > 120_000L
        val isSignificantlyOlder: Boolean = timeDelta < -120_000L
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        val isMoreAccurate = location.accuracy < currentBest.accuracy
        val isSameProvider = location.provider == currentBest.provider

        return isMoreAccurate || (isNewer && isSameProvider)
    }

    @Deprecated("Deprecated in API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
