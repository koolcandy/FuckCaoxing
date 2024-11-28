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

    private lateinit var keyEditText1: EditText
    private lateinit var keyEditText2: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keyEditText1 = findViewById(R.id.keyEditText1)
        keyEditText2 = findViewById(R.id.keyEditText2)
        saveButton = findViewById(R.id.saveButton)
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val savedKey1 = sharedPreferences.getString("key1", "")
        val savedKey2 = sharedPreferences.getString("key2", "")
        keyEditText1.setText(savedKey1)
        keyEditText2.setText(savedKey2)

        saveButton.setOnClickListener {
            val key1 = keyEditText1.text.toString()
            val key2 = keyEditText2.text.toString()
            with(sharedPreferences.edit()) {
                putString("key1", key1)
                putString("key2", key2)
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