package com.obhoy.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

class SmsDispatcher(private val context: Context) {

    fun sendEmergencySms(phoneNumber: String, messageText: String): Boolean {
        // 1. Verify runtime permission before attempting dispatch
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "SEND_SMS permission not granted at runtime. Dispatch aborted.")
            return false
        }

        // 2. Validate phone number format
        if (phoneNumber.isBlank()) {
            Log.e(TAG, "Emergency contact phone number is blank. Dispatch aborted.")
            return false
        }

        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val messageParts = smsManager.divideMessage(messageText)

            if (messageParts.size > 1) {
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    messageParts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    messageText,
                    null,
                    null
                )
            }
            Log.d(TAG, "SMS successfully submitted to network for: $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            false
        }
    }

    companion object {
        private const val TAG = "SmsDispatcher"
    }
}
