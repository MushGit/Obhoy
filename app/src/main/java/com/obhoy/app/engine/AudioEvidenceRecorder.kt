package com.obhoy.app.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

class AudioEvidenceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null

    @Volatile
    private var isRecording = false

    // MediaRecorder must write to a real file path directly — it can't
    // write into an EncryptedFile's stream in real time. So we record to a
    // temporary plaintext file, then immediately re-encrypt it into the
    // vault and delete the temp file the moment recording stops.
    private var pendingTempFile: File? = null
    private var pendingFinalFile: File? = null

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun startRecording(): File? {
        if (isRecording) return null

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val vaultDir = File(context.filesDir, "evidence_vault")
        if (!vaultDir.exists()) vaultDir.mkdirs()

        val tempDir = File(context.cacheDir, "evidence_tmp")
        if (!tempDir.exists()) tempDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val tempFile = File(tempDir, "recording_tmp_$timestamp.aac")
        val final
