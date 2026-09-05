package com.obhoy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.obhoy.app.data.local.entity.LocationHistoryEntity

@Dao
interface LocationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(locationPoint: LocationHistoryEntity)

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocationSync(): LocationHistoryEntity?

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocation(): LocationHistoryEntity?

    @Query("SELECT * FROM location_history WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getLocationTrailSince(sinceTimestamp: Long): List<LocationHistoryEntity>

    @Query("DELETE FROM location_history WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeLocationsOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM location_history")
    suspend fun clearHistory()
}
