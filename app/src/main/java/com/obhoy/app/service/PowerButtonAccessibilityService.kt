package com.obhoy.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.obhoy.app.engine.DispatchManager

class PowerButtonAccessibilityService : AccessibilityService() {

    private var clickCount = 0
    private var lastClickTime: Long = 0
    private val resetThresholdMs = 1500L // Reset counter if idle > 1.5s
    private val requiredClicks = 4

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_POWER && event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime > resetThresholdMs) {
                clickCount = 1
            } else {
                clickCount++
            }

            lastClickTime = currentTime

            if (clickCount >= requiredClicks) {
                clickCount = 0
                triggerEmergencyDispatch()
            }
        }
        return super.onKeyEvent(event)
    }

    private fun triggerEmergencyDispatch() {
        val intent = Intent(this, ObhoyForegroundService::class.java).apply {
            action = ObhoyForegroundService.ACTION_TRIGGER_EMERGENCY
        }
        startForegroundService(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}

