package com.obhoy.app.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.databinding.ActivityManageContactsBinding
import kotlinx.coroutines.launch

class ManageContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageContactsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadContacts()
    }

    private fun loadContacts() {
        val app = application as ObhoyApplication
        lifecycleScope.launch {
            val contacts = app.database.emergencyContactDao().getAllContacts()
            // Populate editable fields or RecyclerView adapter with contacts
        }
    }
}
