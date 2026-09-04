package com.obhoy.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.obhoy.app.service.ObhoyForegroundService

class ScreenToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_SCREEN_ON) {
            val now = System.currentTimeMillis()
            if (now - lastToggleTime < TOGGLE_THRESHOLD_MS) {
                toggleCount++
            } else {
                toggleCount = 1
            }
            lastToggleTime = now

            if (toggleCount >= REQUIRED_TOGGLES) {
                toggleCount = 0
                val serviceIntent = Intent(context, ObhoyForegroundService::class.java).apply {
                    action = ObhoyForegroundService.ACTION_TRIGGER_SOS
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }

    companion object {
        private var toggleCount = 0
        private var lastToggleTime = 0L
        private const val TOGGLE_THRESHOLD_MS = 1500L
        private const val REQUIRED_TOGGLES = 4
    }
}
