package com.obhoy.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.service.ObhoyForegroundService

class ScreenToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON) {
            val now = System.currentTimeMillis()
            
            if (now - lastToggleTime < TOGGLE_THRESHOLD_MS) {
                toggleCount++
            } else {
                toggleCount = 1
            }
            lastToggleTime = now

            Log.d(TAG, "Screen toggle registered. Current count: $toggleCount/$REQUIRED_TOGGLES")

            if (toggleCount >= REQUIRED_TOGGLES) {
                toggleCount = 0
                Log.i(TAG, "Emergency toggle threshold reached ($REQUIRED_TOGGLES clicks). Dispatching alert.")

                val app = context.applicationContext as? ObhoyApplication
                
                // 1. Immediate async execution via Application's DispatchManager
                app?.dispatchManager?.triggerEmergencyDispatch("POWER_BUTTON_QUAD_CLICK")

                // 2. Ensure foreground service is alive for sustained telemetry tracking
                val serviceIntent = Intent(context, ObhoyForegroundService::class.java).apply {
                    this.action = ObhoyForegroundService.ACTION_TRIGGER_SOS
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScreenToggleReceiver"
        private var toggleCount = 0
        private var lastToggleTime = 0L
        private const val TOGGLE_THRESHOLD_MS = 1500L
        private const val REQUIRED_TOGGLES = 4
    }
}
