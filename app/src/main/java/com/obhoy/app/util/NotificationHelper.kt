package com.obhoy.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.obhoy.app.R

object NotificationHelper {

    const val CHANNEL_BACKGROUND_SERVICE = "channel_obhoy_background"
    const val CHANNEL_ACTIVE_ESCORT = "channel_obhoy_escort"
    const val CHANNEL_EMERGENCY_ALERTS = "channel_obhoy_emergency"

    const val NOTIF_ID_BACKGROUND_SERVICE = 1001
    const val NOTIF_ID_ACTIVE_ESCORT = 1002
    const val NOTIF_ID_EMERGENCY = 1003

    /**
     * Initializes all notification channels required by the app.
     * Safe to invoke multiple times as channel creation is idempotent.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Silent / Low Importance Channel for Persistent Background Listener
            val backgroundChannel = NotificationChannel(
                CHANNEL_BACKGROUND_SERVICE,
                "Background Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps hardware power-button trigger active in background"
                setShowBadge(false)
            }

            // 2. Default Importance Channel for Active Escort Dead-Man's Switch
            val escortChannel = NotificationChannel(
                CHANNEL_ACTIVE_ESCORT,
                "Active Escort Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Displays remaining countdown during active escort sessions"
            }

            // 3. High Importance Channel for Active Emergency Alerts
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY_ALERTS,
                "Emergency Status & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications during active distress dispatch"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(backgroundChannel, escortChannel, emergencyChannel)
            )
        }
    }

    /**
     * Builds standard background monitoring service notification.
     */
    fun buildBackgroundServiceNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_BACKGROUND_SERVICE)
            .setContentTitle("Obhoy Protection Active")
            .setContentText("Hardware monitoring operational")
            .setSmallIcon(R.drawable.obhoy)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Builds active escort countdown notification.
     */
    fun buildEscortNotification(context: Context, contentText: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ACTIVE_ESCORT)
            .setContentTitle("Obhoy Active Escort")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.obhoy)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }

    /**
     * Builds high-priority notification for active emergency execution.
     */
    fun buildEmergencyNotification(context: Context, contentText: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_EMERGENCY_ALERTS)
            .setContentTitle("EMERGENCY DISPATCH ACTIVE")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.obhoy)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
    }
}
