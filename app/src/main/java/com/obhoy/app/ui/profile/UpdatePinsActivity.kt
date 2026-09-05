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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePinsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSavePins.setOnClickListener {
            savePins()
        }
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
