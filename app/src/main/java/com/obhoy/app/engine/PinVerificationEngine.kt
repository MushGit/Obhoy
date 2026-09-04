package com.obhoy.app.engine

import at.favre.lib.crypto.bcrypt.BCrypt
import com.obhoy.app.data.local.dao.UserProfileDao
import javax.inject.Inject

sealed class PinResult {
    object TruePin : PinResult()
    object DecoyPin : PinResult()
    object Invalid : PinResult()
}

class PinVerificationEngine(
    private val userProfileDao: UserProfileDao
) {

    suspend fun verifyPin(enteredPin: String): PinResult {
        val profile = userProfileDao.getUserProfile() ?: return PinResult.Invalid

        // Check against True PIN hash
        val trueVerification = BCrypt.verifyer().verify(enteredPin.toCharArray(), profile.truePinHash)
        if (trueVerification.verified) {
            return PinResult.TruePin
        }

        // Check against Decoy PIN hash
        val decoyVerification = BCrypt.verifyer().verify(enteredPin.toCharArray(), profile.decoyPinHash)
        if (decoyVerification.verified) {
            return PinResult.DecoyPin
        }

        return PinResult.Invalid
    }

    companion object {
        fun hashPin(pin: String): String {
            return BCrypt.withDefaults().hashToString(12, pin.toCharArray())
        }
    }
}

