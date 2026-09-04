package com.obhoy.app.util

import kotlin.math.roundToInt

object ElevationFormatter {

    // Standard sea level pressure baseline ~1013.25 hPa
    // Standard floor height baseline ~3.5 meters (~0.4 hPa change per floor)
    fun formatPressureToFloor(currentPressureHpa: Float, baselinePressureHpa: Float = 1013.25f): String {
        val pressureDiff = baselinePressureHpa - currentPressureHpa
        val estimatedMeters = pressureDiff * 8.43f // Roughly 8.43m per hPa at sea level
        val floorNumber = (estimatedMeters / 3.5f).roundToInt()

        return when {
            floorNumber <= 0 -> "Ground/Basement"
            floorNumber == 1 -> "1st Floor"
            floorNumber == 2 -> "2nd Floor"
            floorNumber == 3 -> "3rd Floor"
            else -> "${floorNumber}th Floor (~${estimatedMeters.roundToInt()}m)"
        }
    }
}

