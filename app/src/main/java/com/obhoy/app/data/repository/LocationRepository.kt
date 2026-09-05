package com.obhoy.app.data.repository

import com.obhoy.app.data.local.dao.LocationHistoryDao
import com.obhoy.app.data.local.entity.LocationHistoryEntity
import com.obhoy.app.sensor.BarometerElevationEngine
import com.obhoy.app.sensor.GnssSatelliteEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepository(
    private val locationHistoryDao: LocationHistoryDao,
    private val gnssEngine: GnssSatelliteEngine,
    private val barometerEngine: BarometerElevationEngine
) {

    fun getLatestLocationSync(): LocationHistoryEntity? {
        // 1. Direct memory check from live GNSS Engine
        val liveLocation = gnssEngine.lastKnownLocation
        if (liveLocation != null && liveLocation.latitude != 0.0 && liveLocation.longitude != 0.0) {
            return LocationHistoryEntity(
                latitude = liveLocation.latitude,
                longitude = liveLocation.longitude,
                altitudeMeters = liveLocation.altitude,
                pressureHpa = barometerEngine.currentPressure,
                floorEstimate = barometerEngine.getEstimatedFloor(),
                accuracyMeters = liveLocation.accuracy,
                timestamp = liveLocation.time
            )
        }

        // 2. Fallback to cached point from Room DB
        return locationHistoryDao.getLatestLocationSync()
    }

    suspend fun logCurrentLocationPoint(): LocationHistoryEntity? = withContext(Dispatchers.IO) {
        val lastLocation = gnssEngine.lastKnownLocation ?: return@withContext null
        val pressure = barometerEngine.currentPressure
        val floorEstimate = barometerEngine.getEstimatedFloor()

        val entity = LocationHistoryEntity(
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            altitudeMeters = lastLocation.altitude,
            pressureHpa = pressure,
            floorEstimate = floorEstimate,
            accuracyMeters = lastLocation.accuracy,
            timestamp = System.currentTimeMillis()
        )

        locationHistoryDao.insertLocationPoint(entity)
        entity
    }

    suspend fun getLatestLocation(): LocationHistoryEntity? = withContext(Dispatchers.IO) {
        getLatestLocationSync() ?: locationHistoryDao.getLatestLocation()
    }

    suspend fun getLocationTrailSince(sinceTimestamp: Long): List<LocationHistoryEntity> = withContext(Dispatchers.IO) {
        locationHistoryDao.getLocationTrailSince(sinceTimestamp)
    }

    suspend fun purgeOldBreadcrumbs(cutoffTimestamp: Long) = withContext(Dispatchers.IO) {
        locationHistoryDao.purgeLocationsOlderThan(cutoffTimestamp)
    }
}
