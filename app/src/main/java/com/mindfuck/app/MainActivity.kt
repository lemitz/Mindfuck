package com.mindfuck.app

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.mindfuck.app.ui.AppRoot
import com.mindfuck.app.ui.theme.MindfuckTheme
import com.mindfuck.app.ui.theme.ThemeSettings

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applySystemBars(light = ThemeSettings.light)
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
        setContent {
            val light = ThemeSettings.light
            LaunchedEffect(light) { applySystemBars(light) }
            MindfuckTheme {
                AppRoot()
            }
        }
    }

    /** Status bar always sits on the (blue / dark-blue) top bar → light icons. Nav bar follows the theme. */
    private fun applySystemBars(light: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = if (light) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            else SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.setBackgroundDrawable(ColorDrawable(if (light) Color.WHITE else 0xFF0E1216.toInt()))
    }

    override fun onResume() {
        super.onResume()
        // Some OEM skins reset the mode when the app comes back from background.
        requestHighestRefreshRate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        requestHighestRefreshRate()
    }

    /**
     * Pin the display to its fastest mode (120 Hz / 144 Hz panels) at the current resolution,
     * instead of letting the system drop to 60 Hz for "regular" apps.
     */
    private fun requestHighestRefreshRate() {
        val display = display ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return

        val attrs = window.attributes
        if (attrs.preferredDisplayModeId != best.modeId || attrs.preferredRefreshRate != best.refreshRate) {
            attrs.preferredDisplayModeId = best.modeId
            attrs.preferredRefreshRate = best.refreshRate
            window.attributes = attrs
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Android 15+: ask the ARR heuristics for the high category and boost on touch.
            window.decorView.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
            window.setFrameRateBoostOnTouchEnabled(true)
        }
    }
}
