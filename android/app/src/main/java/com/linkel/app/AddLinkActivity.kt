package com.linkel.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linkel.app.api.NewLink
import com.linkel.app.api.RetrofitClient
import com.linkel.app.databinding.ActivityAddLinkBinding
import kotlinx.coroutines.launch

class AddLinkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLinkBinding
    private var username: String = ""

    companion object {
        const val EXTRA_USERNAME = "extra_username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        username = intent.getStringExtra(EXTRA_USERNAME) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Share a Link"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSubmit.setOnClickListener { submitLink() }
    }

    private fun submitLink() {
        val title = binding.etTitle.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isBlank()) {
            binding.etTitle.error = "Title is required"
            return
        }
        if (url.isBlank()) {
            binding.etUrl.error = "URL is required"
            return
        }

        binding.btnSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                RetrofitClient.api.addLink(
                    NewLink(
                        username = username,
                        title = title,
                        url = url,
                        description = description.ifBlank { null }
                    )
                )
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddLinkActivity,
                    "Failed to submit: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
