package com.avi.stopgooning

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {

    private var lastTriggerTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventPackage = event.packageName?.toString() ?: ""
        if (eventPackage.contains("avi.stopgooning")) return

        // KEY CHANGE: Only trigger when the user is TYPING (text is changing)
        // This stops the app from triggering just because a bad word appeared in an ad
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {

            // Get the text that was just typed
            val typedText = event.text.toString().lowercase()

            if (containsTriggerWord(typedText)) {
                Log.d("Blocker", "User typed a restricted word: $typedText")
                handleDetection()
            }
        }
    }

    private fun containsTriggerWord(text: String): Boolean {
        // Define your list of words here
        val triggers = listOf("goon", "p*rn", "xvideos", "hub", "hot", "sexy")

        // Checks if any trigger word exists inside the typed string
        return triggers.any { text.contains(it, ignoreCase = true) }
    }

    private fun handleDetection() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < 5000) return
        lastTriggerTime = currentTime

        // Save detection state
        val prefs = getSharedPreferences("StopGooningPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hasBeenDetected", true).apply()

        Log.d("Blocker", "Launching Lockdown UI...")

        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("triggeredByDetection", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("Blocker", "Failed to launch: ${e.message}")
        }
    }

    override fun onInterrupt() {}
}