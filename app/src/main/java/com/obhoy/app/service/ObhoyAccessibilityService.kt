package com.obhoy.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.obhoy.app.ui.escort.ActiveEscortActivity

class ObhoyAccessibilityService : AccessibilityService() {

    private var clickCount = 0
    private var lastClickTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events monitoring
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_POWER -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 1000) {
                        clickCount++
                    } else {
                        clickCount = 1
                    }
                    lastClickTime = currentTime

                    // Hardware Quad-Click Trigger (4 fast clicks)
                    if (clickCount >= 4) {
                        clickCount = 0
                        triggerEmergencyAlert()
                    }
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun triggerEmergencyAlert() {
        val intent = Intent(this, ActiveEscortActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
}
