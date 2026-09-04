package com.obhoy.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.obhoy.app.databinding.ActivityProfileBinding
import com.obhoy.app.service.ActiveEscortTimerService
import com.obhoy.app.ui.escort.ActiveEscortActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnableAccessibility.setOnClickListener {
            // Open system accessibility settings page so user can toggle Obhoy's power button listener
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnStartEscort.setOnClickListener {
            val startEscortIntent = Intent(this, ActiveEscortTimerService::class.java).apply {
                action = ActiveEscortTimerService.ACTION_START_TIMER
                putExtra(ActiveEscortTimerService.EXTRA_DURATION_MINUTES, 15) // Default 15 mins
            }
            startForegroundService(startEscortIntent)

            startActivity(Intent(this, ActiveEscortActivity::class.java))
        }
    }
}

