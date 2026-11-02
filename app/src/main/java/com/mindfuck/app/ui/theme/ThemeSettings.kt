package com.mindfuck.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App-controlled theme: dark by default, light is opt-in from Settings (independent of the system). */
object ThemeSettings {
    private const val PREFS = "ui"
    private const val KEY_LIGHT = "light_theme"

    var light by mutableStateOf(false)
        private set

    fun init(context: Context) {
        light = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LIGHT, false)
    }

    fun setLight(context: Context, value: Boolean) {
        light = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_LIGHT, value).apply()
    }
}
