package com.obhoy.app.engine

import com.obhoy.app.data.local.entity.UserProfileEntity

object SmsPayloadCompiler {

    fun compileEmergencySms(
        userProfile: UserProfileEntity,
        latitude: Double,
        longitude: Double,
        elevationFloor: String
    ): String {
        val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
        val name = userProfile.fullName
        val nidPart = userProfile.nationalId?.let { " NID:$it" } ?: ""

        // Keep payload under 160 GSM single-segment characters
        return "SOS! $name$nidPart needs help. Loc: $mapsUrl ($elevationFloor). Sent via Obhoy App."
    }
}

