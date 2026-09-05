package com.obhoy.app.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.R
import com.obhoy.app.data.local.entity.UserProfileEntity
import com.obhoy.app.databinding.FragmentProfileSetupBinding
import kotlinx.coroutines.launch

class ProfileSetupFragment : Fragment() {

    private var _binding: FragmentProfileSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadExistingProfile()

        binding.btnSaveProfile.setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadExistingProfile() {
        val app = requireActivity().application as ObhoyApplication

        lifecycleScope.launch {
            val profile = app.database.userProfileDao().getUserProfile()
            profile?.let {
                binding.etFullName.setText(it.fullName)
                binding.etPrimaryPhone.setText(it.primaryPhone)
                binding.etNationalId.setText(it.nationalId ?: "")
                binding.etHomeAddress.setText(it.homeAddress ?: "")
            }
        }
    }

    private fun saveProfileData() {
        val name = binding.etFullName.text.toString().trim()
        val phone = binding.etPrimaryPhone.text.toString().trim()
        val nid = binding.etNationalId.text.toString().trim().ifEmpty { null }
        val address = binding.etHomeAddress.text.toString().trim().ifEmpty { null }

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Full name and phone number are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPhoneNumber(phone)) {
            Toast.makeText(requireContext(), "Enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        val app = requireActivity().application as ObhoyApplication

        lifecycleScope.launch {
            val existingProfile = app.database.userProfileDao().getUserProfile()

            val updatedProfile = UserProfileEntity(
                id = 1,
                fullName = name,
                primaryPhone = phone,
                nationalId = nid,
                homeAddress = address,
                truePinHash = existingProfile?.truePinHash ?: "",
                decoyPinHash = existingProfile?.decoyPinHash ?: "",
                isSetupComplete = existingProfile?.isSetupComplete ?: false
            )

            app.database.userProfileDao().saveUserProfile(updatedProfile)

            Toast.makeText(requireContext(), "Profile details saved", Toast.LENGTH_SHORT).show()
            
            // Navigate to Emergency Contacts Setup step
            findNavController().navigate(R.id.action_profileSetup_to_contactsSetup)
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val regex = Regex("^(?:\\+?88)?01[3-9]\\d{8}$")
        return regex.matches(phone)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
