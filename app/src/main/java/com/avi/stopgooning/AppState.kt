package com.avi.stopgooning

import android.content.Context
import android.content.SharedPreferences

class AppState(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("StopGooningPrefs", Context.MODE_PRIVATE)

    var hasBeenDetected: Boolean
        get() = prefs.getBoolean("hasBeenDetected", false)
        set(value) = prefs.edit().putBoolean("hasBeenDetected", value).apply()
}