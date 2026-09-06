package com.obhoy.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.obhoy.app.sensor.LocationLoggerWorker
import com.obhoy.app.service.ActiveEscortTimerService
import com.obhoy.app.service.ObhoyForegroundService
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Boot/Replaced action received ($action). Rescheduling background tasks and foreground service.")

            // 1. Reschedule Periodic Location Logging with WorkManager
            scheduleLocationLoggerWork(context)

            // 2. Restart Persistent Background Foreground Monitoring Service
            startBackgroundService(context)

            // 3. Resume an in-progress Active Escort session, if one was
            // running before the reboot. Without this, a device restart
            // silently drops the safety timer with no recovery and no
            // fail-safe dispatch if the window had already expired.
            resumeEscortSessionIfAny(context)
        }
    }

    private fun scheduleLocationLoggerWork(context: Context) {
        val locationWorkRequest = PeriodicWorkRequestBuilder<LocationLoggerWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LocationLoggerWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            locationWorkRequest
        )
    }

    private fun startBackgroundService(context: Context) {
        val serviceIntent = Intent(context, ObhoyForegroundService::class.java).apply {
            action = ObhoyForegroundService.ACTION_START_MONITORING
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ObhoyForegroundService from BootReceiver", e)
        }
    }

    private fun resumeEscortSessionIfAny(context: Context) {
        // No explicit action set — ActiveEscortTimerService's onStartCommand
        // treats a null action as "check for and resume a persisted session,"
        // and is a no-op if none was in progress.
        val escortIntent = Intent(context, ActiveEscortTimerService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(escortIntent)
            } else {
                context.startService(escortIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume ActiveEscortTimerService from BootReceiver", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
