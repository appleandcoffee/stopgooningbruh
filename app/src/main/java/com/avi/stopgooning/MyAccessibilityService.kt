package com.avi.stopgooning

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    private var lastTriggerTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 1. STRENGHTENED FILTER: Ignore EVERYTHING from your app package.
        // Your package name "stopgooning" contains a trigger word,
        // so we must exit immediately if the event is from us.
        val eventPackage = event.packageName?.toString() ?: ""
        if (eventPackage.contains("avi.stopgooning")) {
            return
        }

        // 2. Performance: Only scan on major events like window state changes or scrolls
        // Scanning on TYPE_VIEW_ACCESSIBILITY_FOCUSED (hovering) is too heavy.
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {

            val rootNode = rootInActiveWindow ?: return
            performFastSearch(rootNode)
            rootNode.recycle()
        }
    }

    private fun performFastSearch(rootNode: AccessibilityNodeInfo) {
        val triggers = listOf("goon", "hub", "p*rn") // Add others as needed

        for (keyword in triggers) {
            // findAccessibilityNodeInfosByText is 100x faster than manual recursion
            val foundNodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (foundNodes.isNotEmpty()) {
                Log.d("Blocker", "Trigger found: $keyword")

                // Clean up the list immediately
                for (node in foundNodes) {
                    node.recycle()
                }

                triggerPushups()
                return
            }
        }
    }

    private fun triggerPushups() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < 5000) return

        lastTriggerTime = currentTime
        Log.d("Blocker", "Forcing Activity to Front...")

        try {
            val intent = Intent(this, PushupActivity::class.java).apply {
                // These 4 flags combined are the "Magic Bullet" for background starts
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                // This prevents the "White Fade" transition which can hang on slow CPUs
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("Blocker", "Launch failed: ${e.message}")
        }
    }

    override fun onInterrupt() {}
}