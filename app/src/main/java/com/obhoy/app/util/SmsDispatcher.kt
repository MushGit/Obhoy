package com.obhoy.app.util

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

class SmsDispatcher(private val context: Context) {

    fun sendEmergencySms(phoneNumber: String, messageText: String) {
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
    }
}
