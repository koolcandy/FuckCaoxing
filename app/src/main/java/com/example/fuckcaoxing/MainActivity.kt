package com.example.fuckcaoxing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var keyEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keyEditText = findViewById(R.id.keyEditText)
        saveButton = findViewById(R.id.saveButton)
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val savedKey = sharedPreferences.getString("key", "")
        keyEditText.setText(savedKey)

        saveButton.setOnClickListener {
            val key = keyEditText.text.toString()
            with(sharedPreferences.edit()) {
                putString("key", key)
                apply()
            }
        }

        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingWindowService()
            }
        }

        if (Settings.canDrawOverlays(this)) {
            startFloatingWindowService()
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun startFloatingWindowService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        startService(intent)
    }
}