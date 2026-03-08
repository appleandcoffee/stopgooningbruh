package com.avi.stopgooning

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // Using generic 'View' prevents ClassCastExceptions
    private var dayOneView: View? = null
    private var caughtView: View? = null
    private var streakText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize with safe finding
        dayOneView = findViewById(R.id.dayOneView)
        caughtView = findViewById(R.id.caughtView)
        streakText = findViewById(R.id.streakText)

        val btnPushup = findViewById<Button>(R.id.pushupButton)
        val btnSettings = findViewById<Button>(R.id.btnOpenSettings)

        // 1. Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        // 2. Settings Button
        btnSettings?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Enable 'StopGooning' in Settings", Toast.LENGTH_LONG).show()
        }

        // 3. Pushup Button
        btnPushup?.setOnClickListener {
            val prefs = getSharedPreferences("StopGooningPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("hasBeenDetected", false).apply()

            startActivity(Intent(this, PushupActivity::class.java))
        }

        setupStreak()
    }

    private fun setupStreak() {
        val prefs = getSharedPreferences("StopGooningPrefs", Context.MODE_PRIVATE)
        if (prefs.getLong("streakStartTime", 0L) == 0L) {
            prefs.edit().putLong("streakStartTime", System.currentTimeMillis()).apply()
        }
        updateStreakDisplay()
    }

    private fun updateStreakDisplay() {
        val prefs = getSharedPreferences("StopGooningPrefs", Context.MODE_PRIVATE)
        val startTime = prefs.getLong("streakStartTime", System.currentTimeMillis())
        val diff = System.currentTimeMillis() - startTime
        val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
        streakText?.text = "DAY $days"
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        updateStreakDisplay()
    }

    private fun updateUIState() {
        val prefs = getSharedPreferences("StopGooningPrefs", Context.MODE_PRIVATE)
        val isDetected = prefs.getBoolean("hasBeenDetected", false)

        if (isDetected) {
            dayOneView?.visibility = View.GONE
            caughtView?.visibility = View.VISIBLE
            // Reset streak when caught
            prefs.edit().putLong("streakStartTime", System.currentTimeMillis()).apply()
        } else {
            dayOneView?.visibility = View.VISIBLE
            caughtView?.visibility = View.GONE
        }
    }
}