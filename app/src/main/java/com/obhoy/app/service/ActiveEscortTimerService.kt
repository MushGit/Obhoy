package com.obhoy.app.service

import androidx.core.content.ContextCompat
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
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

    // Persists the absolute end time so a killed/restarted service (or a
    // device reboot, if BootReceiver checks this) can recompute the correct
    // remaining duration instead of silently losing the session.
    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

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
                clearPersistedEndTime()
                stopSelf()
            }
            null -> {
                // Service was restarted by the OS (e.g. after being killed
                // under memory pressure) with no explicit action. Try to
                // resume an in-progress session from the persisted end time
                // rather than silently dropping the safety timer.
                resumeIfSessionInProgress()
            }
        }
        // Changed from START_NOT_STICKY: if the OS kills this service while
        // a session is active, we want it restarted so the fail-safe (silent
        // emergency dispatch on timeout) still has a chance to fire.
        return START_REDELIVER_INTENT
    }

    private fun startCountdown(durationMs: Long) {
        val endTimeMillis = System.currentTimeMillis() + durationMs
        persistEndTime(endTimeMillis)
        beginCountdownUntil(endTimeMillis)
    }

    private fun resumeIfSessionInProgress() {
        val endTimeMillis = prefs.getLong(KEY_END_TIME, -1L)
        if (endTimeMillis <= 0L) return // no session was in progress

        val remaining = endTimeMillis - System.currentTimeMillis()
        if (remaining <= 0L) {
            // Session should already have ended while we were down —
            // fire the fail-safe dispatch immediately rather than dropping it.
            clearPersistedEndTime()
            triggerEmergencyDispatch()
            stopSelf()
        } else {
            beginCountdownUntil(endTimeMillis)
        }
    }

    private fun beginCountdownUntil(endTimeMillis: Long) {
        val notification = buildTimerNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val durationMs = endTimeMillis - System.currentTimeMillis()

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMs.coerceAtLeast(0), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMs = millisUntilFinished

                // In-process notification only — scoped to our own package so
                // no other app on the device can listen in on session state.
                val intent = Intent(ACTION_TIMER_TICK).apply {
                    putExtra(EXTRA_TIME_REMAINING, millisUntilFinished)
                    setPackage(packageName)
                }
                sendBroadcast(intent)

                if (millisUntilFinished in 1000..60000 && (millisUntilFinished / 1000) % 10 == 0L) {
                    triggerWarningVibration()
                }
            }

            override fun onFinish() {
                clearPersistedEndTime()
                triggerEmergencyDispatch()
                stopSelf()
            }
        }.start()
    }

    private fun triggerEmergencyDispatch() {
        val emergencyIntent = Intent(this, ObhoyForegroundService::class.java).apply {
            action = ObhoyForegroundService.ACTION_TRIGGER_EMERGENCY
        }
        ContextCompat.startForegroundService(this, emergencyIntent)
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
    }

    private fun persistEndTime(endTimeMillis: Long) {
        prefs.edit().putLong(KEY_END_TIME, endTimeMillis).apply()
    }

    private fun clearPersistedEndTime() {
        prefs.edit().remove(KEY_END_TIME).apply()
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
            // Generic name — visible in system notification settings,
            // shouldn't hint at what the app or feature actually is.
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Routine background service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildTimerNotification(): Notification {
        // Deliberately generic and static — no app name, no feature name,
        // no countdown. A foreground service notification is mandatory on
        // Android 8+, but its content doesn't have to reveal anything.
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Sync")
            .setContentText("Background service running")
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

        private const val PREFS_NAME = "obhoy_escort_state"
        private const val KEY_END_TIME = "escort_end_time_millis"
    }
}
