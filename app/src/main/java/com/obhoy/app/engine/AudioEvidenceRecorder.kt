package com.obhoy.app.engine

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioEvidenceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    fun startRecording(): File? {
        if (isRecording) return null

        val outputDir = File(context.filesDir, "evidence_vault")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val outputFile = File(outputDir, "audio_evidence_$timestamp.aac")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
                isRecording = true
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }

        return outputFile
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

