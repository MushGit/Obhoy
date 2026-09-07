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

        // Fallback to the most recent point actually persisted to Room.
        return locationHistoryDao.getLatestLocationSync()
    }

    /**
     * Persists a location point to Room. This is the method that makes the
     * DB fallback tier meaningful — it must actually be called periodically
     * (e.g. from a lightweight periodic worker or whenever a fresh fix is
     * obtained elsewhere) or the DB fallback will always be empty.
     */
    suspend fun logCurrentLocationPoint(): LocationHistoryEntity? = withContext(Dispatchers.IO) {
        val lastLocation = gnssEngine.lastKnownLocation ?: return@withContext null

        val entity = LocationHistoryEntity(
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            altitudeMeters = lastLocation.altitude,
            pressureHpa = barometerEngine.currentPressure,
            floorEstimate = barometerEngine.getEstimatedFloor(),
            accuracyMeters = lastLocation.accuracy,
            timestamp = System.currentTimeMillis()
        )

        locationHistoryDao.insertLocationPoint(entity)
        entity
    }

    /**
     * Explicitly persists a given Location (e.g. one just obtained by
     * GnssSatelliteEngine.awaitFreshLocation) rather than relying on
     * gnssEngine.lastKnownLocation being pre-populated. This replaces the
     * old no-op saveLocationToLocalDb() stub that used to live inside
     * GnssSatelliteEngine and never actually wrote anything.
     */
    suspend fun logLocationPoint(location: android.location.Location) = withContext(Dispatchers.IO) {
        val entity = LocationHistoryEntity(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude,
            pressureHpa = barometerEngine.currentPressure,
            floorEstimate = barometerEngine.getEstimatedFloor(),
            accuracyMeters = location.accuracy,
            timestamp = location.time
        )
        locationHistoryDao.insertLocationPoint(entity)
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
