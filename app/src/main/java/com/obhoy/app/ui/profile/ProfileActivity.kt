package com.obhoy.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.R
import com.obhoy.app.data.local.entity.EmergencyContactEntity
import com.obhoy.app.databinding.ActivityProfileBinding
import com.obhoy.app.databinding.ItemContactBinding
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
        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

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
            val contacts = app.database.emergencyContactDao().getAllContacts()

            if (contacts.isEmpty()) {
                Toast.makeText(
                    this@ProfileActivity,
                    "No emergency contacts registered yet.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                binding.rvDashboardContacts.adapter = DashboardContactsAdapter(contacts)
            }
        }
    }

    // Lightweight Inner Adapter to render contacts list
    private class DashboardContactsAdapter(
        private val contactsList: List<EmergencyContactEntity>
    ) : RecyclerView.Adapter<DashboardContactsAdapter.ContactViewHolder>() {

        class ContactViewHolder(val binding: ItemContactBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val binding = ItemContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ContactViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            val contact = contactsList[position]
            // Map Entity fields to item_contact.xml layout views
            holder.binding.run {
                // Modify these view IDs if item_contact.xml uses different names
                tvContactName.text = contact.name
                tvContactPhone.text = contact.phoneNumber
            }
        }

        override fun getItemCount(): Int = contactsList.size
    }
}
