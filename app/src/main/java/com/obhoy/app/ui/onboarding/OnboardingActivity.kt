package com.obhoy.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.databinding.ActivityOnboardingBinding
import com.obhoy.app.ui.profile.ProfileActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ObhoyApplication

        // Bypass onboarding if setup is already complete
        lifecycleScope.launch {
            val existingProfile = app.database.userProfileDao().getUserProfile()
            if (existingProfile != null && existingProfile.isSetupComplete) {
                startActivity(Intent(this@OnboardingActivity, ProfileActivity::class.java))
                finish()
            }
        }
    }
}
