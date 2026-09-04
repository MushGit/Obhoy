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
import com.obhoy.app.engine.DispatchManager
import com.obhoy.app.engine.GnssSatelliteEngine
import com.obhoy.app.engine.LocationLoggerWorker
import com.obhoy.app.util.CryptoUtils
import com.obhoy.app.util.NotificationHelper
import com.obhoy.app.util.SmsDispatcher
import net.sqlcipher.database.SQLiteDatabase
import java.util.concurrent.TimeUnit

class ObhoyApplication : Application() {

    lateinit var database: ObhoyDatabase
        private set

    lateinit var userProfileRepository: UserProfileRepository
        private set
    lateinit var emergencyContactRepository: EmergencyContactRepository
        private set
    lateinit var locationRepository: LocationRepository
        private set

    lateinit var gnssEngine: GnssSatelliteEngine
        private set
    lateinit var barometerEngine: BarometerElevationEngine
        private set

    lateinit var smsDispatcher: SmsDispatcher
        private set
    lateinit var dispatchManager: DispatchManager
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize SQLCipher native C++ libraries before any DB operations
        SQLiteDatabase.loadLibs(this)

        // 2. Setup Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // 3. Initialize Encrypted Room Database
        val passphrase = getOrCreateDatabasePassphrase()
        database = ObhoyDatabase.getInstance(this, passphrase)

        // 4. Initialize Engines & Repositories
        gnssEngine = GnssSatelliteEngine(this)
        barometerEngine = BarometerElevationEngine(this)

        userProfileRepository = UserProfileRepository(database.userProfileDao())
        emergencyContactRepository = EmergencyContactRepository(database.emergencyContactDao())
        locationRepository = LocationRepository(
            database.locationHistoryDao(),
            gnssEngine,
            barometerEngine
        )

        // 5. Initialize Dispatch System Utilities
        smsDispatcher = SmsDispatcher(this)
        dispatchManager = DispatchManager(
            context = this,
            emergencyContactRepository = emergencyContactRepository,
            userProfileRepository = userProfileRepository,
            locationRepository = locationRepository,
            smsDispatcher = smsDispatcher
        )

        // 6. Schedule Background Telemetry Caching
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
            15, TimeUnit.MINUTES
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
