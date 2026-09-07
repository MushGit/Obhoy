package com.obhoy.app.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File

class AudioEvidenceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null

    @Volatile
    private var isRecording = false

    fun startRecording(): File? {
        if (isRecording) return null

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null // Caller should check permission before invoking this
        }

        // TODO: evidence audio is currently stored unencrypted on disk.
        // Given the threat model (device may be accessed by a coercive
        // party), this should be encrypted at rest — e.g. via a
        // Keystore-backed CipherOutputStream — same as the location cache
        // fix applied to LocationLoggerWorker.
        val outputDir = File(context.filesDir, "evidence_vault")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val outputFile = File(outputDir, "audio_evidence_$timestamp.aac")

        return try {
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            outputFile
        } catch (e: Exception) {
            // Catches IOException, IllegalStateException, RuntimeException,
            // and SecurityException — any of which can occur if the
            // recorder is in a bad state, permission is revoked mid-call,
            // or the microphone is unavailable (e.g. in use by another app).
            e.printStackTrace()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }
}
