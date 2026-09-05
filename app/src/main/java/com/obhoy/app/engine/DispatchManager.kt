package com.obhoy.app.engine

import android.content.Context
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

                // 1. Wait up to 3 seconds for a non-zero GPS fix
                val validLocation = awaitValidLocation()
                val lat = validLocation?.latitude ?: 0.0
                val lng = validLocation?.longitude ?: 0.0

                // 2. Fetch real-time weather pressure baseline using valid coordinates
                if (lat != 0.0 && lng != 0.0) {
                    val baseline = weatherRepository.fetchSurfacePressureHpa(lat, lng)
                    if (baseline != null) {
                        app.barometerEngine.updateBaselinePressure(baseline)
                    }
                }

                // 3. Compute accurate floor after pressure baseline update
                val floor = app.barometerEngine.getEstimatedFloor()

                executeSmsDispatch(
                    latitude = lat,
                    longitude = lng,
                    floorEstimate = floor
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute background emergency dispatch trigger", e)
            }
        }
    }

    /**
     * Polls LocationRepository for up to 3000ms until valid coordinates arrive.
     */
    private suspend fun awaitValidLocation() = withTimeoutOrNull(3000L) {
        while (true) {
            val location = locationRepository.getLatestLocationSync()
            if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                return@withTimeoutOrNull location
            }
            delay(200L)
        }
        null
    }

    /**
     * Core dispatch runner using existing repositories and SmsPayloadCompiler logic.
     */
    suspend fun executeSmsDispatch(
        latitude: Double,
        longitude: Double,
        floorEstimate: String
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

        // Maintain existing payload generation contract via SmsPayloadCompiler
        val payload = SmsPayloadCompiler.compileEmergencySms(
            userProfile = userProfile,
            latitude = latitude,
            longitude = longitude,
            floorEstimate = floorEstimate,
            timestamp = System.currentTimeMillis()
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

    companion object {
        private const val TAG = "DispatchManager"
    }
}
