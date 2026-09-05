package com.obhoy.app.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
            } else {
                requestEssentialPermissions()
            }
        }
    }

    private fun requestEssentialPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungrantedPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                ungrantedPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val smsIndex = permissions.indexOf(Manifest.permission.SEND_SMS)
            if (smsIndex != -1 && grantResults.getOrNull(smsIndex) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "SMS permission is required to dispatch emergency SOS messages.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
