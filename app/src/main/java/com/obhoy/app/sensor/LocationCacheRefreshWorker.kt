package com.obhoy.app.sensor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obhoy.app.ObhoyApplication

/**
 * Periodically refreshes the location fallback used by DispatchManager,
 * by writing into the already-encrypted SQLCipher Room database via
 * LocationRepository — replacing the old LocationLoggerWorker, which wrote
 * to a separate, unencrypted SharedPreferences cache that nothing else
 * ever actually read.
 */
class LocationCacheRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as ObhoyApplication
            val result = app.locationRepository.logCurrentLocationPoint()
            if (result != null) Result.success() else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "ObhoyLocationCacheRefresh"
    }
}
