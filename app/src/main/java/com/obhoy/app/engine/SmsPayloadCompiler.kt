package com.obhoy.app.engine

import com.obhoy.app.data.local.entity.UserProfileEntity
import java.util.Locale

object SmsPayloadCompiler {

    private const val GSM7_SEGMENT_LIMIT = 160
    private const val UCS2_SEGMENT_LIMIT = 70

    /**
     * Compiles a concise, high-reliability emergency SMS payload.
     * The location link is always kept intact — if space is tight, the
     * user's name is shortened first, never the coordinates or floor.
     */
    fun compileEmergencySms(
        userProfile: UserProfileEntity,
        latitude: Double?,
        longitude: Double?,
        floorEstimate: String,
        isFallbackLocation: Boolean = false
    ): String {
        val name = userProfile.fullName.trim()

        val locationPayload = when {
            latitude != null && longitude != null && isValidCoordinate(latitude, longitude) -> {
                val formattedLat = String.format(Locale.US, "%.5f", latitude)
                val formattedLng = String.format(Locale.US, "%.5f", longitude)
                val baseLink = "https://maps.google.com/?q=$formattedLat,$formattedLng"
                if (isFallbackLocation) "$baseLink (Last Known)" else baseLink
            }
            else -> "Signal Blocked (No Fix)"
        }

        // Floor is presented as an estimate, since it may be refined and
        // resent moments later once weather-corrected pressure data arrives.
        val floorPayload = floorEstimate.ifBlank { "Unknown" }.let { "~$it (est.)" }

        val limit = segmentLimitFor(name)

        // Fixed portion that must never be cut: everything except the name.
        val suffix = " needs help! Loc: $locationPayload Floor: $floorPayload"
        val prefix = "EMERGENCY! "
        val fixedLength = prefix.length + suffix.length

        val availableForName = (limit - fixedLength).coerceAtLeast(0)
        val safeName = if (name.length > availableForName) {
            if (availableForName > 1) name.take(availableForName - 1) + "…" else ""
        } else {
            name
        }

        return "$prefix$safeName$suffix"
    }

    /**
     * A message containing any non-GSM-7 character (e.g. Bangla script)
     * will be sent as UCS-2, which caps a single segment at 70 characters
     * instead of 160. Using the correct limit avoids an unexpected,
     * unintended multi-part split.
     */
    private fun segmentLimitFor(text: String): Int {
        val isGsm7Compatible = text.all { it.code in 0x20..0x7E || it == '\n' || it == '\r' }
        return if (isGsm7Compatible) GSM7_SEGMENT_LIMIT else UCS2_SEGMENT_LIMIT
    }

    private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
        return true
    }
}
