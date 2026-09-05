package com.obhoy.app.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.data.local.entity.EmergencyContactEntity
import com.obhoy.app.databinding.FragmentContactsSetupBinding
import kotlinx.coroutines.launch

class ContactsSetupFragment : Fragment() {

    private var _binding: FragmentContactsSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        loadExistingContacts()
    }

    private fun setupListeners() {
        binding.btnAddContact.setOnClickListener {
            val name = binding.etContactName.text.toString().trim()
            val phone = binding.etContactPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both name and phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPhoneNumber(phone)) {
                Toast.makeText(requireContext(), "Enter a valid Bangladeshi phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveContact(name, phone)
        }
    }

    private fun saveContact(name: String, phone: String) {
        val app = requireActivity().application as ObhoyApplication

        lifecycleScope.launch {
            val currentContacts = app.database.emergencyContactDao().getAllContacts()
            if (currentContacts.size >= 5) {
                Toast.makeText(requireContext(), "Maximum of 5 emergency contacts allowed", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val newContact = EmergencyContactEntity(
                name = name,
                phoneNumber = phone,
                priorityOrder = currentContacts.size + 1
            )

            app.database.emergencyContactDao().insertContact(newContact)
            
            // Clear input fields
            binding.etContactName.text?.clear()
            binding.etContactPhone.text?.clear()

            Toast.makeText(requireContext(), "Contact added successfully", Toast.LENGTH_SHORT).show()
            loadExistingContacts()
        }
    }

    private fun loadExistingContacts() {
        val app = requireActivity().application as ObhoyApplication

        lifecycleScope.launch {
            val contacts = app.database.emergencyContactDao().getAllContacts()
            
            // Update UI list or counter indicator
            binding.tvContactCount.text = "${contacts.size} / 5 Contacts Added"
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Validates standard BD numbers (e.g., +8801700000000 or 01700000000)
        val regex = Regex("^(?:\\+?88)?01[3-9]\\d{8}$")
        return regex.matches(phone)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
