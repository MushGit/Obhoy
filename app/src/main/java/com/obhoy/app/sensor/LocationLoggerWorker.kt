package com.obhoy.app.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationLoggerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        return try {
            // Attempt to fetch a fresh high-accuracy location fix with a 5-second timeout
            val cancellationTokenSource = CancellationTokenSource()
            val freshLocation: Location? = withTimeoutOrNull(5000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            }

            val targetLocation = freshLocation ?: fusedLocationClient.lastLocation.await()

            if (targetLocation != null) {
                saveLocationToLocalCache(targetLocation)
                Result.success()
            } else {
                // Could not retrieve location fix within threshold
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun saveLocationToLocalCache(location: Location) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().apply {
            putFloat(KEY_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LONGITUDE, location.longitude.toFloat())
            putLong(KEY_TIMESTAMP, location.time)
            putFloat(KEY_ACCURACY, location.accuracy)
            apply()
        }
    }

    companion object {
        const val WORK_NAME = "ObhoyPeriodicLocationLogger"
        private const val PREFS_NAME = "obhoy_location_cache"
        const val KEY_LATITUDE = "cached_latitude"
        const val KEY_LONGITUDE = "cached_longitude"
        const val KEY_TIMESTAMP = "cached_timestamp"
        const val KEY_ACCURACY = "cached_accuracy"

        /**
         * Returns the encrypted SharedPreferences instance for the location
         * cache. IMPORTANT: any other file that previously read this cache
         * via context.getSharedPreferences(PREFS_NAME, ...) directly must be
         * updated to call this method instead — a plain getSharedPreferences
         * call will now return a different, empty, unencrypted preference
         * file, since the data written here is encrypted under a different
         * underlying store.
         */
        fun getEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
