package com.obhoy.app.engine

import android.content.Context
import android.telephony.SmsManager
import com.obhoy.app.data.local.ObhoyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DispatchManager(private val context: Context) {

    private val db = (context.applicationContext as com.obhoy.app.ObhoyApplication).database

    fun executeEmergencyDispatch(latitude: Double, longitude: Double, elevationFloor: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val profile = db.userProfileDao().getUserProfile() ?: return@launch
            val contacts = db.emergencyContactDao().getAllContacts()

            if (contacts.isEmpty()) return@launch

            val messagePayload = SmsPayloadCompiler.compileEmergencySms(
                userProfile = profile,
                latitude = latitude,
                longitude = longitude,
                elevationFloor = elevationFloor
            )

            val smsManager = context.getSystemService(SmsManager::class.java)

            for (contact in contacts) {
                try {
                    val parts = smsManager.divideMessage(messagePayload)
                    smsManager.sendMultipartTextMessage(
                        contact.phoneNumber,
                        null,
                        parts,
                        null,
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

