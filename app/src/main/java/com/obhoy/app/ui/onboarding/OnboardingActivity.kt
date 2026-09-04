package com.obhoy.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.data.local.entity.EmergencyContactEntity
import com.obhoy.app.data.local.entity.UserProfileEntity
import com.obhoy.app.databinding.ActivityOnboardingBinding
import com.obhoy.app.engine.PinVerificationEngine
import com.obhoy.app.ui.profile.ProfileActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ObhoyApplication

        // Check if user has already onboarded
        lifecycleScope.launch {
            val existingProfile = app.database.userProfileDao().getUserProfile()
            if (existingProfile != null && existingProfile.isSetupComplete) {
                startActivity(Intent(this@OnboardingActivity, ProfileActivity::class.java))
                finish()
            }
        }

        binding.btnSaveAndComplete.setOnClickListener {
            executeRegistration(app)
        }
    }

    private fun executeRegistration(app: ObhoyApplication) {
        val name = binding.etFullName.text.toString().trim()
        val phone = binding.etPrimaryPhone.text.toString().trim()
        val truePin = binding.etTruePin.text.toString().trim()
        val decoyPin = binding.etDecoyPin.text.toString().trim()
        val contactPhone = binding.etEmergencyContactPhone.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || truePin.length < 4 || decoyPin.length < 4 || contactPhone.isEmpty()) {
            Toast.makeText(this, "Fill all required fields & 4-digit PINs", Toast.LENGTH_SHORT).show()
            return
        }

        if (truePin == decoyPin) {
            Toast.makeText(this, "True PIN and Decoy PIN must be different", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Hash PINs securely via BCrypt
            val trueHash = PinVerificationEngine.hashPin(truePin)
            val decoyHash = PinVerificationEngine.hashPin(decoyPin)

            // Save Profile
            val profile = UserProfileEntity(
                fullName = name,
                primaryPhone = phone,
                truePinHash = trueHash,
                decoyPinHash = decoyHash,
                isSetupComplete = true
            )
            app.database.userProfileDao().saveUserProfile(profile)

            // Save Initial Emergency Contact
            val contact = EmergencyContactEntity(
                name = "Primary Contact",
                phoneNumber = contactPhone,
                priorityOrder = 1
            )
            app.database.emergencyContactDao().insertContact(contact)

            Toast.makeText(this@OnboardingActivity, "Obhoy configured successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@OnboardingActivity, ProfileActivity::class.java))
            finish()
        }
    }
}

