package com.obhoy.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.R
import com.obhoy.app.databinding.ActivityProfileBinding
import com.obhoy.app.service.ActiveEscortTimerService
import com.obhoy.app.service.ObhoyForegroundService
import com.obhoy.app.ui.escort.ActiveEscortActivity
import com.obhoy.app.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigationDrawer()
        setupActionButtons()
        observeEmergencyContacts()
    }

    private fun setupNavigationDrawer() {
        // Top App Bar Menu Hamburger Click Listener
        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Drawer Items Click Listener
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_manage_contacts -> {
                    val intent = Intent(this, OnboardingActivity::class.java).apply {
                        putExtra("NAVIGATE_TO", "CONTACTS")
                    }
                    startActivity(intent)
                }
                R.id.nav_update_pins -> {
                    val intent = Intent(this, OnboardingActivity::class.java).apply {
                        putExtra("NAVIGATE_TO", "PINS")
                    }
                    startActivity(intent)
                }
                R.id.nav_hardware_settings -> {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupActionButtons() {
        binding.btnTriggerSos.setOnClickListener {
            val emergencyIntent = Intent(this, ObhoyForegroundService::class.java).apply {
                action = ObhoyForegroundService.ACTION_TRIGGER_SOS
            }
            ContextCompat.startForegroundService(this, emergencyIntent)
            Toast.makeText(this, "Emergency Dispatch Activated", Toast.LENGTH_SHORT).show()
        }

        binding.btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnStartEscort.setOnClickListener {
            val startEscortIntent = Intent(this, ActiveEscortTimerService::class.java).apply {
                action = ActiveEscortTimerService.ACTION_START_TIMER
                putExtra(ActiveEscortTimerService.EXTRA_DURATION_MINUTES, 15)
            }
            ContextCompat.startForegroundService(this, startEscortIntent)
            startActivity(Intent(this, ActiveEscortActivity::class.java))
        }
    }

    private fun observeEmergencyContacts() {
        val app = application as ObhoyApplication
        binding.rvDashboardContacts.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val contacts = app.database.userProfileDao().getAllEmergencyContacts()
            if (contacts.isEmpty()) {
                Toast.makeText(
                    this@ProfileActivity,
                    "No emergency contacts registered yet.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Attach contacts adapter to populated list
                // binding.rvDashboardContacts.adapter = ContactsAdapter(contacts)
            }
        }
    }
}
