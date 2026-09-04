package com.obhoy.app.engine

import android.content.Context
import android.util.Log
import com.obhoy.app.data.repository.EmergencyContactRepository
import com.obhoy.app.data.repository.LocationRepository
import com.obhoy.app.data.repository.UserProfileRepository
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

    /**
     * Entry point triggered asynchronously by receivers or accessibility service.
     */
    fun triggerEmergencyDispatch(triggerType: String = "HARDWARE_POWER_TOGGLE") {
        scope.launch {
            try {
                val lastKnownLocation = locationRepository.getLatestLocationSync()
                val lat = lastKnownLocation?.latitude ?: 0.0
                val lng = lastKnownLocation?.longitude ?: 0.0
                val floor = lastKnownLocation?.estimatedFloor?.let { "Floor $it" } ?: "Ground"

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

        var deliveredCount = 0
        for (contact in contacts) {
            val success = smsDispatcher.sendEmergencySms(
                phoneNumber = contact.phoneNumber,
                message = payload
            )
            if (success) deliveredCount++
        }

        Log.i(TAG, "Dispatch sequence complete. Sent $deliveredCount/${contacts.size} messages.")
    }

    companion object {
        private const val TAG = "DispatchManager"
    }
}
