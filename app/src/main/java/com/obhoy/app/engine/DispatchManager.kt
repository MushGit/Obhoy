package com.obhoy.app.engine

import android.content.Context
import android.location.Location
import android.util.Log
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.data.repository.EmergencyContactRepository
import com.obhoy.app.data.repository.LocationRepository
import com.obhoy.app.data.repository.UserProfileRepository
import com.obhoy.app.data.repository.WeatherRepository
import com.obhoy.app.util.SmsDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DispatchManager(
    private val context: Context,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val userProfileRepository: UserProfileRepository,
    private val locationRepository: LocationRepository,
    private val smsDispatcher: SmsDispatcher
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val weatherRepository = WeatherRepository()

    /**
     * Entry point triggered asynchronously by receivers or accessibility service.
     */
    fun triggerEmergencyDispatch(triggerType: String = "HARDWARE_POWER_TOGGLE") {
        scope.launch {
            try {
                val app = context.applicationContext as ObhoyApplication

                // 1. Fetch best available location via GnssSatelliteEngine
                var isFallback = false
                val freshLocation: Location? = app.gnssEngine.awaitFreshLocation(timeoutMs = 45_000L)

                var lat: Double? = freshLocation?.latitude
                var lng: Double? = freshLocation?.longitude

                // 2. If live satellite acquisition fails, fallback to last saved DB location entity
                if (lat == null || lng == null || !isValidCoordinate(lat, lng)) {
                    val dbLocationEntity = locationRepository.getLatestLocationSync()
                    if (dbLocationEntity != null && isValidCoordinate(dbLocationEntity.latitude, dbLocationEntity.longitude)) {
                        lat = dbLocationEntity.latitude
                        lng = dbLocationEntity.longitude
                        isFallback = true
                    } else {
                        lat = null
                        lng = null
                    }
                }

                // 3. Fetch real-time weather pressure baseline if coordinates are valid
                if (lat != null && lng != null) {
                    val baseline = weatherRepository.fetchSurfacePressureHpa(lat, lng)
                    if (baseline != null) {
                        app.barometerEngine.updateBaselinePressure(baseline)
                    }
                }

                // 4. Compute accurate floor after pressure baseline update
                val floor = app.barometerEngine.getEstimatedFloor()

                executeSmsDispatch(
                    latitude = lat,
                    longitude = lng,
                    floorEstimate = floor,
                    isFallbackLocation = isFallback
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute background emergency dispatch trigger", e)
            }
        }
    }

    /**
     * Core dispatch runner using existing repositories and SmsPayloadCompiler logic.
     */
    suspend fun executeSmsDispatch(
        latitude: Double?,
        longitude: Double?,
        floorEstimate: String,
        isFallbackLocation: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val userProfile = userProfileRepository.getUserProfileSync() ?: run {
            Log.w(TAG, "Dispatch aborted: User profile not found.")
            return@withContext
        }

        val contacts = emergencyContactRepository.getEmergencyContactsSync()
        if (contacts.isEmpty()) {
            Log.w(TAG, "Dispatch aborted: No emergency contacts configured.")
            return@withContext
        }

        // Updated signature call matching SmsPayloadCompiler
        val payload = SmsPayloadCompiler.compileEmergencySms(
            userProfile = userProfile,
            latitude = latitude,
            longitude = longitude,
            floorEstimate = floorEstimate,
            isFallbackLocation = isFallbackLocation
        )

        var dispatchedCount = 0
        for (contact in contacts) {
            try {
                smsDispatcher.sendEmergencySms(
                    phoneNumber = contact.phoneNumber,
                    messageText = payload
                )
                dispatchedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS to ${contact.phoneNumber}", e)
            }
        }

        Log.i(TAG, "Dispatch sequence complete. Sent $dispatchedCount/${contacts.size} messages.")
    }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        if (lat == 0.0 && lng == 0.0) return false
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return false
        return true
    }

    companion object {
        private const val TAG = "DispatchManager"
    }
}
