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
        val finalFile = File(vaultDir, "audio_evidence_$timestamp.aac.enc")

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
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            pendingTempFile = tempFile
            pendingFinalFile = finalFile
            finalFile // caller gets the path the encrypted file will live at
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            tempFile.delete()
            null
        }
    }

    /**
     * Stops recording, encrypts the captured audio into the evidence vault,
     * and deletes the plaintext temp file. Returns the final encrypted
     * file, or null if stopping/encryption failed.
     */
    fun stopRecording(): File? {
        if (!isRecording) return null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder = null
            isRecording = false
            pendingTempFile?.delete()
            pendingTempFile = null
            pendingFinalFile = null
            return null
        }

        mediaRecorder = null
        isRecording = false

        val tempFile = pendingTempFile
        val finalFile = pendingFinalFile
        pendingTempFile = null
        pendingFinalFile = null

        if (tempFile == null || finalFile == null || !tempFile.exists()) return null

        return try {
            if (finalFile.exists()) finalFile.delete()

            val encryptedFile = EncryptedFile.Builder(
                context,
                finalFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            tempFile.delete() // plaintext copy no longer needed
            finalFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypts and returns the raw bytes of a previously recorded evidence
     * file. Caller is responsible for handling the returned bytes securely
     * (e.g. not writing them back to plain disk outside decryptToTempFile).
     */
    fun readEncryptedRecording(file: File): ByteArray? {
        return try {
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileInput().use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lists all saved encrypted recordings, most recent first.
     */
    fun listSavedRecordings(): List<File> {
        val vaultDir = File(context.filesDir, "evidence_vault")
        if (!vaultDir.exists()) return emptyList()
        return vaultDir.listFiles { f -> f.extension == "enc" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Decrypts a saved recording into a short-lived plaintext copy inside
     * the app's cache directory, for playback or sharing. The caller is
     * responsible for deleting this file when done (see deleteTempFile).
     * This is the ONLY point where decrypted audio touches disk on its
     * own — it never leaves app-private cache storage automatically.
     */
    fun decryptToTempFile(encryptedFile: File): File? {
        return try {
            val tempDir = File(context.cacheDir, "share_tmp")
            if (!tempDir.exists()) tempDir.mkdirs()

            val tempFile = File(tempDir, encryptedFile.nameWithoutExtension + ".aac")
            if (tempFile.exists()) tempFile.delete()

            val bytes = readEncryptedRecording(encryptedFile) ?: return null
            tempFile.writeBytes(bytes)
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteTempFile(tempFile: File) {
        try {
            if (tempFile.exists()) tempFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
