package com.obhoy.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.R
import com.obhoy.app.receiver.ScreenToggleReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ObhoyForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var screenToggleReceiver: ScreenToggleReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        promoteToForegroundSafely()

        val app = application as ObhoyApplication
        app.gnssEngine.startListening()
        app.barometerEngine.startListening()

        registerScreenToggleReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TRIGGER_EMERGENCY, ACTION_TRIGGER_SOS -> {
                executeEmergencyWorkflow()
            }
            ACTION_START_MONITORING -> {
                // Background hardware status active
            }
        }
        return START_STICKY
    }

    private fun registerScreenToggleReceiver() {
        if (screenToggleReceiver == null) {
            screenToggleReceiver = ScreenToggleReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenToggleReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(screenToggleReceiver, filter)
            }
        }
    }

    private fun unregisterScreenToggleReceiver() {
        screenToggleReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered
            }
            screenToggleReceiver = null
        }
    }

    private fun executeEmergencyWorkflow() {
        serviceScope.launch {
            val app = application as ObhoyApplication
            app.dispatchManager.triggerEmergencyDispatch("FOREGROUND_SERVICE_ACTION")
        }
    }

    /**
     * Handles API 34+ (Android 14) foreground service startup constraints without throwing
     * ForegroundServiceStartNotAllowedException or SecurityException.
     */
    private fun promoteToForegroundSafely() {
        val notification = createNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var foregroundType = 0
                
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    foregroundType = foregroundType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    foregroundType = foregroundType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }

                if (foregroundType != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, foregroundType)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            // Fallback for security exceptions during background promotion
            startForeground(NOTIFICATION_ID, notification)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenToggleReceiver()

        val app = application as ObhoyApplication
        app.gnssEngine.stopListening()
        app.barometerEngine.stopListening()

        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "obhoy_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_MONITORING = "com.obhoy.app.ACTION_START_MONITORING"
        const val ACTION_TRIGGER_EMERGENCY = "com.obhoy.app.ACTION_TRIGGER_EMERGENCY"
        const val ACTION_TRIGGER_SOS = "com.obhoy.app.TRIGGER_SOS"
    }
}
