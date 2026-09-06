package com.obhoy.app.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.databinding.ActivityUpdatePinsBinding
import com.obhoy.app.security.PinVerificationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdatePinsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdatePinsBinding
    private val pinEngine = PinVerificationEngine()

    // True only after the user has re-entered their current PIN correctly.
    // The new-PIN fields stay disabled until this is true.
    private var isReauthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePinsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Lock the new-PIN fields until re-auth succeeds.
        setNewPinFieldsEnabled(false)

        binding.btnVerifyCurrentPin.setOnClickListener {
            verifyCurrentPin()
        }

        binding.btnSavePins.setOnClickListener {
            if (isReauthenticated) {
                savePins()
            } else {
                Toast.makeText(this, "Please confirm your current PIN first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyCurrentPin() {
        val enteredPin = binding.etCurrentPin.text.toString().trim()
        if (enteredPin.isEmpty()) {
            Toast.makeText(this, "Enter your current PIN", Toast.LENGTH_SHORT).show()
            return
        }

        val app = application as ObhoyApplication
        lifecycleScope.launch {
            val currentProfile = withContext(Dispatchers.IO) {
                app.database.userProfileDao().getUserProfile()
            }

            if (currentProfile == null) {
                Toast.makeText(this@UpdatePinsActivity, "User profile not found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // verifyPin() already switches to Dispatchers.Default internally,
            // so it's called directly here without wrapping it again.
            val matches = pinEngine.verifyPin(enteredPin, currentProfile.truePinHash)

            if (matches) {
                isReauthenticated = true
                setNewPinFieldsEnabled(true)
                binding.etCurrentPin.isEnabled = false
                binding.btnVerifyCurrentPin.isEnabled = false
                Toast.makeText(this@UpdatePinsActivity, "Verified — enter new PINs", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@UpdatePinsActivity, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setNewPinFieldsEnabled(enabled: Boolean) {
        binding.etSafePin.isEnabled = enabled
        binding.etDuressPin.isEnabled = enabled
        binding.btnSavePins.isEnabled = enabled
    }

    private fun savePins() {
        val safePinInput = binding.etSafePin.text.toString().trim()
        val duressPinInput = binding.etDuressPin.text.toString().trim()

        if (safePinInput.length < 4 || duressPinInput.length < 4) {
            Toast.makeText(this, "PINs must be at least 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        if (safePinInput == duressPinInput) {
            Toast.makeText(this, "Safe PIN and Duress PIN cannot be identical", Toast.LENGTH_SHORT).show()
            return
        }

        val app = application as ObhoyApplication
        lifecycleScope.launch {
            val currentProfile = withContext(Dispatchers.IO) {
                app.database.userProfileDao().getUserProfile()
            }

            if (currentProfile != null) {
                val hashedTruePin = pinEngine.hashPin(safePinInput)
                val hashedDecoyPin = pinEngine.hashPin(duressPinInput)

                val updatedProfile = currentProfile.copy(
                    truePinHash = hashedTruePin,
                    decoyPinHash = hashedDecoyPin
                )

                withContext(Dispatchers.IO) {
                    app.database.userProfileDao().saveUserProfile(updatedProfile)
                }
                Toast.makeText(this@UpdatePinsActivity, "PINs updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@UpdatePinsActivity, "User profile not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
