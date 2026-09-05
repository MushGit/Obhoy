package com.obhoy.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.databinding.FragmentPinSetupBinding
import com.obhoy.app.engine.PinVerificationEngine
import com.obhoy.app.ui.profile.ProfileActivity
import kotlinx.coroutines.launch

class PinSetupFragment : Fragment() {

    private var _binding: FragmentPinSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSavePins.setOnClickListener {
            validateAndSavePins()
        }
    }

    private fun validateAndSavePins() {
        val truePin = binding.etTruePin.text.toString().trim()
        val confirmTruePin = binding.etConfirmTruePin.text.toString().trim()
        val decoyPin = binding.etDecoyPin.text.toString().trim()
        val confirmDecoyPin = binding.etConfirmDecoyPin.text.toString().trim()

        // Validation Checks
        if (truePin.length < 4 || decoyPin.length < 4) {
            Toast.makeText(requireContext(), "PINs must be at least 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        if (truePin != confirmTruePin) {
            Toast.makeText(requireContext(), "True PINs do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (decoyPin != confirmDecoyPin) {
            Toast.makeText(requireContext(), "Decoy PINs do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (truePin == decoyPin) {
            Toast.makeText(requireContext(), "Decoy PIN cannot be identical to True PIN", Toast.LENGTH_SHORT).show()
            return
        }

        val app = requireActivity().application as ObhoyApplication

        lifecycleScope.launch {
            val existingProfile = app.database.userProfileDao().getUserProfile()

            if (existingProfile == null) {
                Toast.makeText(requireContext(), "Complete profile setup first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Hash PINs securely via BCrypt before saving to SQLCipher vault
            val trueHash = PinVerificationEngine.hashPin(truePin)
            val decoyHash = PinVerificationEngine.hashPin(decoyPin)

            val updatedProfile = existingProfile.copy(
                truePinHash = trueHash,
                decoyPinHash = decoyHash,
                isSetupComplete = true
            )

            app.database.userProfileDao().saveUserProfile(updatedProfile)

            Toast.makeText(requireContext(), "Obhoy setup complete!", Toast.LENGTH_SHORT).show()

            // Complete registration by opening the main ProfileActivity
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
