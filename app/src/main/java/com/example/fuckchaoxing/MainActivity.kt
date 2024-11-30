package com.example.fuckchaoxing

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var keyEditText1: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keyEditText1 = findViewById(R.id.keyEditText1)
        saveButton = findViewById(R.id.saveButton)
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val savedKey1 = sharedPreferences.getString("key", "")
        keyEditText1.setText(savedKey1)

        saveButton.setOnClickListener {
            val key = keyEditText1.text.toString()
            with(sharedPreferences.edit()) {
                putString("key", key)
                apply()
            }
        }
    }


    companion object {
        fun getScreenResolution(context: Context): Pair<Int, Int> {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val metrics = windowManager.currentWindowMetrics
            val width = metrics.bounds.width()
            val height = metrics.bounds.height()
            return Pair(width, height)
        }
    }
}