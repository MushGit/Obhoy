package com.obhoy.app.ui.escort

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.obhoy.app.ObhoyApplication
import com.obhoy.app.databinding.ActivityActiveEscortBinding
import com.obhoy.app.engine.PinVerificationEngine
import com.obhoy.app.engine.PinVerificationResult
import com.obhoy.app.service.ActiveEscortTimerService
import com.obhoy.app.service.ObhoyForegroundService
import kotlinx.coroutines.launch

class ActiveEscortActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActiveEscortBinding
    private lateinit var pinEngine: PinVerificationEngine

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ActiveEscortTimerService.ACTION_TIMER_TICK) {
                val millisRemaining = intent.getLongExtra(ActiveEscortTimerService.EXTRA_TIME_REMAINING, 0L)
                updateTimerUi(millisRemaining)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActiveEscortBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ObhoyApplication
        pinEngine = PinVerificationEngine(app.database.userProfileDao())

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ActiveEscortTimerService.ACTION_TIMER_TICK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(timerReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(timerReceiver)
    }

    private fun setupClickListeners() {
        val handleDisarm: () -> Unit = {
            val enteredPin = binding.etCheckinPin.text.toString().trim()
            if (enteredPin.isEmpty()) {
                Toast.makeText(this, "Enter PIN", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    when (pinEngine.verifyPin(enteredPin)) {
                        is PinVerificationResult.TruePinSuccess -> {
                            // Stop timer and close
                            val cancelIntent = Intent(this@ActiveEscortActivity, ActiveEscortTimerService::class.java).apply {
                                action = ActiveEscortTimerService.ACTION_CANCEL_TIMER
                            }
                            startService(cancelIntent)
                            Toast.makeText(this@ActiveEscortActivity, "Disarmed safely", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is PinVerificationResult.DecoyPinSuccess -> {
                            // Coercion Defense: Stop timer visually, launch believable "Safe" screen, BUT trigger background SOS silently
                            val cancelIntent = Intent(this@ActiveEscortActivity, ActiveEscortTimerService::class.java).apply {
                                action = ActiveEscortTimerService.ACTION_CANCEL_TIMER
                            }
                            startService(cancelIntent)

                            // Trigger silent dispatch safely across API levels
                            val sosIntent = Intent(this@ActiveEscortActivity, ObhoyForegroundService::class.java).apply {
                                action = ObhoyForegroundService.ACTION_TRIGGER_EMERGENCY
                            }
                            ContextCompat.startForegroundService(this@ActiveEscortActivity, sosIntent)

                            // Redirect to decoy landing
                            startActivity(Intent(this@ActiveEscortActivity, DecoySafeActivity::class.java))
                            finish()
                        }
                        is PinVerificationResult.InvalidPin -> {
                            Toast.makeText(this@ActiveEscortActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                            binding.etCheckinPin.text?.clear()
                        }
                    }
                }
            }
        }

        binding.btnCheckIn.setOnClickListener { handleDisarm() }
        binding.btnStopEscort.setOnClickListener { handleDisarm() }
    }

    private fun updateTimerUi(millisRemaining: Long) {
        val totalSeconds = millisRemaining / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.tvCountdownTimer.text = String.format("%02d:%02d", minutes, seconds)
    }
}
