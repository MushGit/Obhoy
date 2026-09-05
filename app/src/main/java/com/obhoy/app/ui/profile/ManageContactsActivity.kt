package com.obhoy.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.data.local.entity.EmergencyContactEntity
import com.obhoy.app.databinding.ActivityManageContactsBinding
import com.obhoy.app.databinding.ItemContactBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageContactsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddContact.setOnClickListener {
            addContact()
        }

        setupRecyclerView()
        loadContacts()
    }

    private fun setupRecyclerView() {
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
    }

    private fun loadContacts() {
        val app = application as ObhoyApplication
        lifecycleScope.launch {
            val contacts = withContext(Dispatchers.IO) {
                app.database.emergencyContactDao().getAllContacts()
            }
            binding.rvContacts.adapter = ContactsAdapter(
                contactsList = contacts,
                onDeleteClick = { contact -> deleteContact(contact) }
            )
        }
    }

    private fun addContact() {
        val name = binding.etContactName.text.toString().trim()
        val phone = binding.etContactPhone.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val app = application as ObhoyApplication
        lifecycleScope.launch {
            val currentCount = withContext(Dispatchers.IO) {
                app.database.emergencyContactDao().getAllContacts().size
            }
            
            withContext(Dispatchers.IO) {
                app.database.emergencyContactDao().insertContact(
                    EmergencyContactEntity(
                        name = name,
                        phoneNumber = phone,
                        priorityOrder = currentCount + 1
                    )
                )
            }
            binding.etContactName.setText("")
            binding.etContactPhone.setText("")
            Toast.makeText(this@ManageContactsActivity, "Contact added", Toast.LENGTH_SHORT).show()
            loadContacts()
        }
    }

    private fun deleteContact(contact: EmergencyContactEntity) {
        val app = application as ObhoyApplication
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                app.database.emergencyContactDao().deleteContact(contact)
            }
            Toast.makeText(this@ManageContactsActivity, "Contact removed", Toast.LENGTH_SHORT).show()
            loadContacts()
        }
    }

    private class ContactsAdapter(
        private val contactsList: List<EmergencyContactEntity>,
        private val onDeleteClick: (EmergencyContactEntity) -> Unit
    ) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = contactsList[position]
            holder.binding.run {
                tvContactName.text = contact.name
                tvContactPhone.text = contact.phoneNumber
                root.setOnClickListener {
                    onDeleteClick(contact)
                }
            }
        }

        override fun getItemCount(): Int = contactsList.size
    }
}
