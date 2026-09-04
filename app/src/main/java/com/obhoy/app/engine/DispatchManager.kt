package com.obhoy.app.engine

import android.content.Context
import android.telephony.SmsManager
import com.obhoy.app.data.local.ObhoyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DispatchManager(
    private val context: Context,
    private val database: ObhoyDatabase
) {

    suspend fun executeSmsDispatch(
        latitude: Double,
        longitude: Double,
        floorEstimate: String
    ) = withContext(Dispatchers.IO) {
        val userProfile = database.userProfileDao().getUserProfile() ?: return@withContext
        val contacts = database.emergencyContactDao().getAllContacts()

        if (contacts.isEmpty()) return@withContext

        val payload = SmsPayloadCompiler.compileEmergencySms(
            userProfile = userProfile,
            latitude = latitude,
            longitude = longitude,
            floorEstimate = floorEstimate,
            timestamp = System.currentTimeMillis()
        )

        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        for (contact in contacts) {
            try {
                smsManager.sendTextMessage(
                    contact.phoneNumber,
                    null,
                    payload,
                    null,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
