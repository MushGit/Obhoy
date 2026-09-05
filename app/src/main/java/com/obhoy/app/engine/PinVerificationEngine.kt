package com.obhoy.app.engine

import at.favre.lib.crypto.bcrypt.BCrypt
import com.obhoy.app.data.local.dao.UserProfileDao

sealed class PinVerificationResult {
    object TruePinSuccess : PinVerificationResult()
    object DecoyPinSuccess : PinVerificationResult()
    object InvalidPin : PinVerificationResult()
}

class PinVerificationEngine(
    private val userProfileDao: UserProfileDao
) {

    suspend fun verifyPin(enteredPin: String): PinVerificationResult {
        if (enteredPin.isBlank()) return PinVerificationResult.InvalidPin

        val profile = userProfileDao.getUserProfile() ?: return PinVerificationResult.InvalidPin

        // Verify True PIN
        if (!profile.truePinHash.isNullOrBlank()) {
            val trueResult = BCrypt.verifyer().verify(enteredPin.toCharArray(), profile.truePinHash)
            if (trueResult.verified) {
                return PinVerificationResult.TruePinSuccess
            }
        }

        // Verify Decoy PIN
        if (!profile.decoyPinHash.isNullOrBlank()) {
            val decoyResult = BCrypt.verifyer().verify(enteredPin.toCharArray(), profile.decoyPinHash)
            if (decoyResult.verified) {
                return PinVerificationResult.DecoyPinSuccess
            }
        }

        return PinVerificationResult.InvalidPin
    }

    companion object {
        private const val BCRYPT_COST = 12

        fun hashPin(pin: String): String {
            return BCrypt.withDefaults().hashToString(BCRYPT_COST, pin.toCharArray())
        }
    }
}
