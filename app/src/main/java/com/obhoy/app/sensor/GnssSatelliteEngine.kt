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
        var bestLocation: Location? = null

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                1f,
                this
            )
            bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                2000L,
                1f,
                this
            )
            val netLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (bestLocation == null || (netLocation != null && netLocation.time > bestLocation.time)) {
                bestLocation = netLocation
            }
        }

        lastKnownLocation = bestLocation
    }

    fun stopListening() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        if (lastKnownLocation == null || location.accuracy <= lastKnownLocation!!.accuracy || location.time > lastKnownLocation!!.time) {
            lastKnownLocation = location
        }
    }

    @Deprecated("Deprecated in API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
