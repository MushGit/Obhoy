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

    fun triggerEmergencyDispatch(triggerType: String = "HARDWARE_POWER_TOGGLE") {
        scope.launch {
            try {
                val app = context.applicationContext as ObhoyApplication

                // 1. Get the best DB-backed location entity first, so it can
                // be passed as a real fallback (previously this was never
                // supplied, so that fallback tier was always dead).
                val dbLocationEntity = locationRepository.getLatestLocationSync()
                val dbFallbackLocation: Location? = dbLocationEntity?.let {
                    Location("obhoy_db_fallback").apply {
                        latitude = it.latitude
                        longitude = it.longitude
                        time = it.timestamp
                    }
                }

                // 2. Attempt a live fix, now with a real fallback wired in.
                var isFallback = false
                val freshLocation: Location? = app.gnssEngine.awaitFreshLocation(
                    timeoutMs = 45_000L,
                    dbFallbackLocation = dbFallbackLocation
                )

                var lat: Double? = freshLocation?.latitude
                var lng: Double? = freshLocation?.longitude

                if (lat == null || lng == null || !isValidCoordinate(lat, lng)) {
                    lat = null
                    lng = null
                } else if (freshLocation === dbFallbackLocation) {
                    isFallback = true
                }

                // Persist this fix (if valid) so future dispatches have a
                // fresher DB fallback than whatever was used just now.
                if (freshLocation != null && lat != null) {
                    locationRepository.logLocationPoint(freshLocation)
                }

                // 3. Send the emergency SMS immediately with best-available
                // location. Do NOT wait on the weather-corrected floor
                // estimate — that's a refinement, not a requirement, and
                // must never delay the life-safety message.
                val initialFloor = app.barometerEngine.getEstimatedFloor()
                executeSmsDispatch(
                    latitude = lat,
                    longitude = lng,
                    floorEstimate = initialFloor,
                    isFallbackLocation = isFallback
                )

                // 4. Only after the SMS is already sent, optionally refine
                // the floor estimate using weather data, bounded by a short
                // timeout so a slow/unreachable API can't hang this coroutine
                // indefinitely. If it meaningfully changes the floor, send a
                // single follow-up correction.
                if (lat != null && lng != null) {
                    val baseline = withTimeoutOrNull(8_000L) {
                        weatherRepository.fetchSurfacePressureHpa(lat, lng)
                    }
                    if (baseline != null) {
                        app.barometerEngine.updateBaselinePressure(baseline)
                        val refinedFloor = app.barometerEngine.getEstimatedFloor()
                        if (refinedFloor != initialFloor) {
                            executeSmsDispatch(
                                latitude = lat,
                                longitude = lng,
                                floorEstimate = refinedFloor,
                                isFallbackLocation = isFallback,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute background emergency dispatch trigger", e)
            }
        }
    }

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
