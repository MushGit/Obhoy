package com.obhoy.app.engine

import com.obhoy.app.data.local.entity.UserProfileEntity

object SmsPayloadCompiler {

    fun compileEmergencySms(
        userProfile: UserProfileEntity,
        latitude: Double,
        longitude: Double,
        floorEstimate: String,
        timestamp: Long
    ): String {
        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val name = userProfile.fullName
        val nid = userProfile.nationalId?.let { " NID:$it" } ?: ""

        // Keep concise to fit standard 160-character GSM limits
        return "EMERGENCY! $name$nid needs help! Location: $mapsLink Floor: $floorEstimate"
    }
}
