package com.obhoy.app.ui.recorder

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.obhoy.app.databinding.ActivityRecordingsListBinding
import com.obhoy.app.databinding.ItemRecordingBinding
import com.obhoy.app.engine.AudioEvidenceRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class RecordingsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordingsListBinding
    private lateinit var recorder: AudioEvidenceRecorder
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlaybackTempFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recorder = AudioEvidenceRecorder(this)

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        loadRecordings()
    }

    private fun loadRecordings() {
        val recordings = recorder.listSavedRecordings()
        binding.rvRecordings.layoutManager = LinearLayoutManager(this)
        binding.rvRecordings.adapter = RecordingsAdapter(
            recordings,
            onPlayClick = { file -> playRecording(file) },
            onShareClick = { file -> shareRecording(file) }
        )

        binding.tvEmptyState.visibility = if (recordings.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun playRecording(encryptedFile: File) {
        stopPlayback() // stop anything already playing first

        val tempFile = recorder.decryptToTempFile(encryptedFile)
        if (tempFile == null) {
            Toast.makeText(this, "Could not open recording", Toast.LENGTH_SHORT).show()
            return
        }

        currentPlaybackTempFile = tempFile
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener { stopPlayback() }
                prepare()
                start()
            }
            Toast.makeText(this, "Playing…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Playback failed", Toast.LENGTH_SHORT).show()
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        currentPlaybackTempFile?.let { recorder.deleteTempFile(it) }
        currentPlaybackTempFile = null
    }

    private fun shareRecording(encryptedFile: File) {
        val tempFile = recorder.decryptToTempFile(encryptedFile)
        if (tempFile == null) {
            Toast.makeText(this, "Could not prepare recording for sharing", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            tempFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/aac"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Send recording via"))

        // Note: we can't reliably know exactly when the receiving app has
        // finished reading the shared file, so the temp copy is left in the
        // app's private cache directory (not accessible to other apps
        // beyond the one-time grant above) rather than deleted immediately.
        // It will be cleared on next cache cleanup / app restart cleanup below.
    }

    private fun formatTimestamp(file: File): String {
        val format = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
        return format.format(file.lastModified())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        // Clean up any leftover share temp files from this session.
        File(cacheDir, "share_tmp").listFiles()?.forEach { it.delete() }
    }

    private inner class RecordingsAdapter(
        private val files: List<File>,
        private val onPlayClick: (File) -> Unit,
        private val onShareClick: (File) -> Unit
    ) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

        inner class RecordingViewHolder(val binding: ItemRecordingBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
            val binding = ItemRecordingBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return RecordingViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
            val file = files[position]
            holder.binding.tvRecordingTimestamp.text = formatTimestamp(file)
            holder.binding.btnPlay.setOnClickListener { onPlayClick(file) }
            holder.binding.btnShare.setOnClickListener { onShareClick(file) }
        }

        override fun getItemCount(): Int = files.size
    }
}
