package com.obhoy.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.obhoy.app.R

class ActiveEscortTimerService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingMs: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 15)
                startCountdown(durationMinutes * 60 * 1000L)
            }
            ACTION_CANCEL_TIMER -> {
                cancelCountdown()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(durationMs: Long) {
        val notification = buildTimerNotification("Escort active: ${durationMs / 60000} min remaining")

        // Explicitly declare FOREGROUND_SERVICE_TYPE_LOCATION on API 29+ / Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMs = millisUntilFinished
                
                // Broadcast update to ActiveEscortActivity UI
                val intent = Intent(ACTION_TIMER_TICK).apply {
                    putExtra(EXTRA_TIME_REMAINING, millisUntilFinished)
                }
                sendBroadcast(intent)

                // Haptic feedback warning in the final 60 seconds
                if (millisUntilFinished in 1000..60000 && (millisUntilFinished / 1000) % 10 == 0L) {
                    triggerWarningVibration()
                }
            }

            override fun onFinish() {
                // Timer expired without safe PIN entry -> Trigger Silent Emergency Dispatch
                val emergencyIntent = Intent(this@ActiveEscortTimerService, ObhoyForegroundService::class.java).apply {
                    action = ObhoyForegroundService.ACTION_TRIGGER_EMERGENCY
                }
                ContextCompat.startForegroundService(this@ActiveEscortTimerService, emergencyIntent)
                stopSelf()
            }
        }.start()
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
    }

    private fun triggerWarningVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator?.vibrate(
                android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(500)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Obhoy Active Escort",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active safety timer monitoring"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildTimerNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Obhoy Active Escort")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.obhoy)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "obhoy_escort_channel"
        const val NOTIFICATION_ID = 1002
        
        const val ACTION_START_TIMER = "com.obhoy.app.ACTION_START_TIMER"
        const val ACTION_CANCEL_TIMER = "com.obhoy.app.ACTION_CANCEL_TIMER"
        const val ACTION_TIMER_TICK = "com.obhoy.app.ACTION_TIMER_TICK"
        
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_TIME_REMAINING = "extra_time_remaining"
    }
}
