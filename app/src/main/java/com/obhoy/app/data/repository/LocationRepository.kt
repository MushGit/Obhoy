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
        locationHistoryDao.getLatestLocation()
    }

    suspend fun getLocationTrailSince(sinceTimestamp: Long): List<LocationHistoryEntity> = withContext(Dispatchers.IO) {
        locationHistoryDao.getLocationTrailSince(sinceTimestamp)
    }

    suspend fun purgeOldBreadcrumbs(cutoffTimestamp: Long) = withContext(Dispatchers.IO) {
        locationHistoryDao.purgeLocationsOlderThan(cutoffTimestamp)
    }
}
