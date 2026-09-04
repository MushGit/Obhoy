package com.obhoy.app.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class GnssSatelliteEngine(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    @Volatile
    var lastKnownLocation: Location? = null
        private set

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, // Check every 2 seconds
                1f,    // Or 1 meter change
                this
            )
            // Initial cached fix
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
    }

    fun stopListening() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        lastKnownLocation = location
    }

    @Deprecated("Deprecated in API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}

