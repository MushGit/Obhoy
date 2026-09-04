package com.obhoy.app.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class PinVerificationEngine {

    suspend fun hashPin(pin: String): String = withContext(Dispatchers.Default) {
        BCrypt.hashpw(pin, BCrypt.gensalt(12))
    }

    suspend fun verifyPin(inputPin: String, hashedPin: String): Boolean = withContext(Dispatchers.Default) {
        if (hashedPin.isEmpty()) return@withContext false
        try {
            BCrypt.checkpw(inputPin, hashedPin)
        } catch (e: Exception) {
            false
        }
    }
}
