package com.obhoy.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.obhoy.app.data.local.ObhoyDatabase
import com.obhoy.app.data.repository.EmergencyContactRepository
import com.obhoy.app.data.repository.LocationRepository
import com.obhoy.app.data.repository.UserProfileRepository
import com.obhoy.app.engine.BarometerElevationEngine
import com.obhoy.app.engine.GnssSatelliteEngine
import com.obhoy.app.engine.LocationLoggerWorker
import com.obhoy.app.util.CryptoUtils
import com.obhoy.app.util.NotificationHelper
import java.util.concurrent.TimeUnit

class ObhoyApplication : Application() {

    lateinit var database: ObhoyDatabase
        private set

    // Repositories
    lateinit var userProfileRepository: UserProfileRepository
        private set
    lateinit var emergencyContactRepository: EmergencyContactRepository
        private set
    lateinit var locationRepository: LocationRepository
        private set

    // Sensor Engines
    lateinit var gnssEngine: GnssSatelliteEngine
        private set
    lateinit var barometerEngine: BarometerElevationEngine
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // 2. Initialize Encrypted SQLCipher Database
        val passphrase = getOrCreateDatabasePassphrase()
        database = ObhoyDatabase.getInstance(this, passphrase)

        // 3. Initialize Engines & Repositories
        gnssEngine = GnssSatelliteEngine(this)
        barometerEngine = BarometerElevationEngine(this)

        userProfileRepository = UserProfileRepository(database.userProfileDao())
        emergencyContactRepository = EmergencyContactRepository(database.emergencyContactDao())
        locationRepository = LocationRepository(
            database.locationHistoryDao(),
            gnssEngine,
            barometerEngine
        )

        // 4. Schedule Periodic Background Location Logger
        scheduleLocationLoggerWork()
    }

    private fun getOrCreateDatabasePassphrase(): ByteArray {
        val prefs = getSharedPreferences(PREFS_VAULT_KEYS, MODE_PRIVATE)
        val existingKeyBase64 = prefs.getString(KEY_DB_PASSPHRASE, null)

        return if (existingKeyBase64 != null) {
            CryptoUtils.fromBase64(existingKeyBase64)
        } else {
            val newPassphrase = CryptoUtils.generateSecureRandomBytes(32)
            val base64Key = CryptoUtils.toBase64(newPassphrase)
            prefs.edit().putString(KEY_DB_PASSPHRASE, base64Key).apply()
            newPassphrase
        }
    }

    private fun scheduleLocationLoggerWork() {
        val locationWorkRequest = PeriodicWorkRequestBuilder<LocationLoggerWorker>(
            15, TimeUnit.MINUTES // Minimum interval for WorkManager
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            LocationLoggerWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            locationWorkRequest
        )
    }

    companion object {
        private const val PREFS_VAULT_KEYS = "obhoy_secure_vault_keys"
        private const val KEY_DB_PASSPHRASE = "db_sqlcipher_passphrase"
    }
}
