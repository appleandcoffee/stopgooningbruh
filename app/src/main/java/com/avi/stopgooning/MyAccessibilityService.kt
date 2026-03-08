package com.avi.stopgooning

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    private var lastTriggerTime: Long = 0L

    companion object {
        private const val TAG = "GoonBlockSvc"
        // Better triggers: more specific, avoid common false positives
        private val TRIGGER_KEYWORDS = listOf(
            "goon", "gooning", "gooner", "edging", "porn", "pornhub", "xvideos",
            "coom", "cum", "fap", "onlyfans", "nsfw", "hentai"
            // Add leetspeak if needed: "g00n", "c0m", "3dg1ng"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Skip our own app to avoid loops
        if (event.packageName?.toString()?.contains("avi.stopgooning") == true) return

        val relevantTypes = listOf(
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED
        )

        if (event.eventType !in relevantTypes) return

        // Quick check: event text (sometimes partial)
        val eventText = event.text?.joinToString(" ")?.lowercase() ?: ""
        if (containsTrigger(eventText)) {
            Log.d(TAG, "Quick detect in event text: $eventText")
            triggerDetection()
            return
        }

        // Full scan: look at root for editable nodes (most reliable)
        rootInActiveWindow?.let { root ->
            scanNodeTree(root)
        }
    }

    private fun scanNodeTree(node: AccessibilityNodeInfo) {
        if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
            val text = node.text?.toString()?.lowercase() ?: ""
            if (text.isNotEmpty() && containsTrigger(text)) {
                Log.d(TAG, "Detected in editable node: $text")
                triggerDetection()
                return
            }
        }

        // Also check contentDescription (some use it)
        node.contentDescription?.toString()?.lowercase()?.let { desc ->
            if (containsTrigger(desc)) {
                Log.d(TAG, "Detected in contentDesc: $desc")
                triggerDetection()
                return
            }
        }

        // Recurse children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                scanNodeTree(child)
                // No recycle() needed anymore
            }
        }
    }

    private fun containsTrigger(text: String): Boolean {
        val lower = " $text "  // pad for word boundaries
        return TRIGGER_KEYWORDS.any { keyword ->
            lower.contains(" $keyword ") || lower.contains("$keyword ")
        }
    }

    private fun triggerDetection() {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 6000) { // 6 sec cooldown to avoid spam
            Log.d(TAG, "Detection throttled")
            return
        }
        lastTriggerTime = now

        Log.w(TAG, "🚨 GOONING DETECTED - Setting flag & launching main")

        val prefs = getSharedPreferences("StopGooningPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hasBeenDetected", true).apply()

        // Launch your main activity (safer flags)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity", e)
            // Fallback: just set flag, let user open app manually
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connected - monitoring for triggers")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }
}