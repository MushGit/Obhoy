package com.obhoy.app.engine

import at.favre.lib.crypto.bcrypt.BCrypt
import com.obhoy.app.data.local.dao.UserProfileDao
import javax.inject.Inject

sealed class PinVerificationResult {
    object TruePinSuccess : PinVerificationResult()
    object DecoyPinSuccess : PinVerificationResult()
    object InvalidPin : PinVerificationResult()
}

class PinVerificationEngine @Inject constructor(
    private val userProfileDao: UserProfileDao
) {

    suspend fun verifyPin(enteredPin: String): PinVerificationResult {
        val profile = userProfileDao.getUserProfile() ?: return PinVerificationResult.InvalidPin

        // Verify True PIN
        val trueResult = BCrypt.verifyer().verify(enteredPin.toCharArray(), profile.truePinHash)
        if (trueResult.verified) {
            return PinVerificationResult.TruePinSuccess
        }

        // Verify Decoy PIN
        val decoyResult = BCrypt.verifyer().verify(enteredPin.toCharArray(), profile.decoyPinHash)
        if (decoyResult.verified) {
            return PinVerificationResult.DecoyPinSuccess
        }

        return PinVerificationResult.InvalidPin
    }

    companion object {
        fun hashPin(pin: String): String {
            return BCrypt.withDefaults().hashToString(12, pin.toCharArray())
        }
    }
}
