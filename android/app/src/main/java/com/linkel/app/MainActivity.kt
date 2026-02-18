package com.linkel.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.linkel.app.api.DeleteRequest
import com.linkel.app.api.Link
import com.linkel.app.api.RetrofitClient
import com.linkel.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: LinkAdapter
    private var currentUsername: String = ""

    companion object {
        private const val PREFS = "linkel_prefs"
        private const val KEY_USERNAME = "username"
        private const val REQUEST_ADD_LINK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        currentUsername = getPrefs().getString(KEY_USERNAME, "") ?: ""
        if (currentUsername.isBlank()) {
            promptUsername()
        } else {
            initUI()
        }
    }

    private fun initUI() {
        supportActionBar?.title = "Linkel"
        supportActionBar?.subtitle = "@$currentUsername"

        adapter = LinkAdapter(
            currentUsername = currentUsername,
            onLinkClick = { link -> openUrl(link.url) },
            onDeleteClick = { link -> confirmDelete(link) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadLinks() }

        binding.fabAddLink.setOnClickListener {
            val intent = Intent(this, AddLinkActivity::class.java)
            intent.putExtra(AddLinkActivity.EXTRA_USERNAME, currentUsername)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_ADD_LINK)
        }

        loadLinks()
    }

    private fun loadLinks() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val links = RetrofitClient.api.getLinks()
                adapter.submitList(links)
                if (links.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load links: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmDelete(link: Link) {
        AlertDialog.Builder(this)
            .setTitle("Delete link?")
            .setMessage("\"${link.title}\" will be removed.")
            .setPositiveButton("Delete") { _, _ -> deleteLink(link) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLink(link: Link) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteLink(link.id, DeleteRequest(currentUsername))
                if (response.isSuccessful) {
                    loadLinks()
                } else {
                    Toast.makeText(this@MainActivity, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptUsername() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter your username"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Welcome to Linkel")
            .setMessage("Choose a username to get started:")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    promptUsername()
                } else {
                    currentUsername = name
                    getPrefs().edit().putString(KEY_USERNAME, name).apply()
                    initUI()
                }
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_username -> {
                promptUsername()
                true
            }
            R.id.action_refresh -> {
                loadLinks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_LINK && resultCode == RESULT_OK) {
            loadLinks()
        }
    }

    private fun getPrefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
