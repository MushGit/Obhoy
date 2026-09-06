package com.obhoy.app.engine

import com.obhoy.app.data.local.entity.UserProfileEntity
import java.util.Locale

object SmsPayloadCompiler {

    /**
     * Compiles a concise, high-reliability emergency SMS payload.
     * Guaranteed to stay under 160 characters to fit a single GSM SMS segment.
     */
    fun compileEmergencySms(
        userProfile: UserProfileEntity,
        latitude: Double?,
        longitude: Double?,
        floorEstimate: String,
        isFallbackLocation: Boolean = false
    ): String {
        val name = userProfile.fullName.trim()
        val nid = userProfile.nationalId?.takeIf { it.isNotBlank() }?.let { " NID:$it" } ?: ""

        val locationPayload = when {
            // Check for valid non-zero coordinates
            latitude != null && longitude != null && isValidCoordinate(latitude, longitude) -> {
                val formattedLat = String.format(Locale.US, "%.5f", latitude)
                val formattedLng = String.format(Locale.US, "%.5f", longitude)
                val baseLink = "https://maps.google.com/?q=$formattedLat,$formattedLng"
                if (isFallbackLocation) "$baseLink (Last Known)" else baseLink
            }
            // Offline signal blocked / null location handling
            else -> "Signal Blocked (No Fix)"
        }

        val floorPayload = floorEstimate.ifBlank { "Unknown" }

        // Compile payload targeting standard GSM 7-bit 160 char limit
        val payload = "EMERGENCY! $name$nid needs help! Loc: $locationPayload Floor: $floorPayload"

        // Hard truncation safety fallback if user name is exceptionally long
        return if (payload.length > 160) {
            "EMERGENCY! $name needs help! Loc: $locationPayload Floor: $floorPayload".take(160)
        } else {
            payload
        }
    }

    private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
        return true
    }
}
