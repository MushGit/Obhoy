package com.obhoy.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_history")
data class LocationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val pressureHpa: Float? = null,
    val floorEstimate: String? = null,
    val accuracyMeters: Float,
    val timestamp: Long = System.currentTimeMillis()
)
