package com.linkel.app

import android.content.Context
import android.content.Intent
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
        private const val PREFS = "linkel_prefs"
        private const val KEY_USERNAME = "username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Username may come from MainActivity via EXTRA_USERNAME, or fall back to
        // SharedPreferences when this activity is launched via a share intent.
        username = intent.getStringExtra(EXTRA_USERNAME)
            ?: getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_USERNAME, "") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Share a Link"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Pre-fill fields when launched from a share intent (ACTION_SEND)
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
            // The shared text is usually the URL; subject is often the page title
            if (sharedText.isNotBlank()) binding.etUrl.setText(sharedText)
            if (sharedSubject.isNotBlank()) binding.etTitle.setText(sharedSubject)
        }

        binding.btnSubmit.setOnClickListener { submitLink() }
    }

    private fun submitLink() {
        val title = binding.etTitle.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (username.isBlank()) {
            Toast.makeText(this, "No username set — open the main app first", Toast.LENGTH_LONG).show()
            return
        }
        if (title.isBlank()) {
            binding.etTitle.error = "Title is required"
            return
        }
        if (url.isBlank()) {
            binding.etUrl.error = "URL is required"
            return
        }

        // Ensure URL has a scheme so the server's URL validation passes
        val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        binding.btnSubmit.isEnabled = false

        lifecycleScope.launch {
            try {
                RetrofitClient.api.addLink(
                    NewLink(
                        username = username,
                        title = title,
                        url = normalizedUrl,
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
