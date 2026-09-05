package com.obhoy.app.ui.escort

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.obhoy.app.databinding.ActivityDecoySafeBinding

class DecoySafeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecoySafeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecoySafeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Toggles the disguise notepad to simulate real note-taking
        binding.fabAddNote.setOnClickListener {
            binding.llEmptyState.visibility = View.GONE
            binding.etDummyNote.visibility = View.VISIBLE
            binding.etDummyNote.requestFocus()
        }
    }
}
