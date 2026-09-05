package com.obhoy.app.ui.escort

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.obhoy.app.databinding.ActivityDecoySafeBinding

class DecoySafeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecoySafeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecoySafeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Indistinguishable "Safe" confirmation UI to satisfy coercers
        binding.btnReturnHome.setOnClickListener {
            finish()
        }
    }
}
