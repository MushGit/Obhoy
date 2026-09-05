package com.obhoy.app.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.databinding.ActivityUpdatePinsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdatePinsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdatePinsBinding

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

        loadUserPins()
    }

    private fun loadUserPins() {
        val app = application as ObhoyApplication
        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                app.database.userProfileDao().getUserProfile()
            }
            profile?.let {
                binding.etSafePin.setText(it.primaryPin)
                binding.etDuressPin.setText(it.stealthPin)
            }
        }
    }

    private fun savePins() {
        val safePin = binding.etSafePin.text.toString().trim()
        val duressPin = binding.etDuressPin.text.toString().trim()

        if (safePin.length < 4 || duressPin.length < 4) {
            Toast.makeText(this, "PINs must be at least 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        if (safePin == duressPin) {
            Toast.makeText(this, "Safe PIN and Duress PIN cannot be identical", Toast.LENGTH_SHORT).show()
            return
        }

        val app = application as ObhoyApplication
        lifecycleScope.launch {
            val currentProfile = withContext(Dispatchers.IO) {
                app.database.userProfileDao().getUserProfile()
            }

            if (currentProfile != null) {
                val updatedProfile = currentProfile.copy(
                    primaryPin = safePin,
                    stealthPin = duressPin
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
