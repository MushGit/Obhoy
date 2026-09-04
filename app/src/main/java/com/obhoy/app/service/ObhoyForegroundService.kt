package com.obhoy.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.obhoy.app.R

class ObhoyForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TRIGGER_EMERGENCY) {
            executeEmergencyWorkflow()
        }
        return START_STICKY
    }

    private fun executeEmergencyWorkflow() {
        // TODO: Hook into DispatchManager to collect GNSS/Barometer data & send SMS
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Obhoy Protection Active")
            .setContentText("Hardware monitors operational")
            .setSmallIcon(R.drawable.obhoy)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Obhoy Background Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "obhoy_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TRIGGER_EMERGENCY = "com.obhoy.app.ACTION_TRIGGER_EMERGENCY"
    }
}

