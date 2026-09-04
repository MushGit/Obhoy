package com.obhoy.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.obhoy.app.engine.LocationLoggerWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            // 1. Reschedule Periodic Location Logging with WorkManager
            scheduleLocationLoggerWork(context)

            // 2. Restart Persistent Background Foreground Monitoring Service
            startBackgroundService(context)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
