package com.obhoy.app.util

import android.telephony.SmsManager

class SmsDispatcher {

    fun sendEmergencySms(phoneNumber: String, messageText: String) {
        val smsManager = SmsManager.getDefault()
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
