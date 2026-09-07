package com.obhoy.app.ui.recorder

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.obhoy.app.databinding.ActivityAudioRecorderBinding
import com.obhoy.app.engine.AudioEvidenceRecorder

class AudioRecorderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioRecorderBinding
    private lateinit var recorder: AudioEvidenceRecorder
    private var isCurrentlyRecording = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginRecording()
        } else {
            Toast.makeText(this, "Microphone permission is required to record", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecorderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recorder = AudioEvidenceRecorder(this)

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        updateUiState(recording = false)

        binding.btnRecord.setOnClickListener {
            if (isCurrentlyRecording) {
                stopRecording()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun beginRecording() {
        val file = recorder.startRecording()
        if (file != null) {
            isCurrentlyRecording = true
            updateUiState(recording = true)
        } else {
            Toast.makeText(this, "Could not start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val encryptedFile = recorder.stopRecording()
        isCurrentlyRecording = false
        updateUiState(recording = false)
        if (encryptedFile != null) {
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Recording failed to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUiState(recording: Boolean) {
        binding.tvRecordingStatus.text = if (recording) "Recording…" else "Not recording"
        binding.btnRecord.text = if (recording) "Stop" else "Start Recording"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isCurrentlyRecording) {
            recorder.stopRecording()
        }
    }
}
