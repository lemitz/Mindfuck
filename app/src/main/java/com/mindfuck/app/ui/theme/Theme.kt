@file:OptIn(ExperimentalTextApi::class)

package com.mindfuck.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mindfuck.app.R

private fun rc(weight: FontWeight, wght: Int) =
    Font(R.font.realchoice, weight, variationSettings = FontVariation.Settings(FontVariation.weight(wght)))

/** Realme Real Choice everywhere. CSS-ish weights mapped onto the font's own axis. */
val RealChoice = FontFamily(
    rc(FontWeight.Light, 420),
    rc(FontWeight.Normal, AppFonts.REGULAR),
    rc(FontWeight.Medium, AppFonts.MEDIUM),
    rc(FontWeight.SemiBold, 780),
    rc(FontWeight.Bold, AppFonts.BOLD),
)

private fun TextStyle.rc() = copy(fontFamily = RealChoice)

private val AppTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.rc(), displayMedium = displayMedium.rc(), displaySmall = displaySmall.rc(),
        headlineLarge = headlineLarge.rc(), headlineMedium = headlineMedium.rc(), headlineSmall = headlineSmall.rc(),
        titleLarge = titleLarge.rc(), titleMedium = titleMedium.rc(), titleSmall = titleSmall.rc(),
        bodyLarge = bodyLarge.rc(), bodyMedium = bodyMedium.rc(), bodySmall = bodySmall.rc(),
        labelLarge = labelLarge.rc(), labelMedium = labelMedium.rc(), labelSmall = labelSmall.rc(),
    )
}

val Brand = Color(0xFF2196F3)

/** Colors of the eMoods-style level marks (none, mild, moderate, severe) and app chrome. */
@Immutable
data class MoodColors(
    val dark: Boolean,
    val levels: List<Color>,
    val onLevels: List<Color>,
    val yes: Color,
    val missing: Color,
    val sleepBar: Color,
    val grid: Color,
    val chartText: Color,
    val chartBackground: Color,
    val topBar: Color,
    val onTopBar: Color,
)

private val LightMood = MoodColors(
    dark = false,
    levels = listOf(Color(0xFFB2EBF2), Color(0xFF4DD8E6), Color(0xFF3B8DAE), Color(0xFF2E3B66)),
    onLevels = listOf(Color(0xFF12323A), Color(0xFF0C2F36), Color.White, Color.White),
    yes = Color(0xFF3B8DAE),
    missing = Color(0xFF9DB7CC),
    sleepBar = Color(0xFFA9C0D3),
    grid = Color(0xFFE2E6EA),
    chartText = Color(0xFF6B7178),
    chartBackground = Color(0xFFF4F5F6),
    topBar = Brand,
    onTopBar = Color.White,
)

private val DarkMood = MoodColors(
    dark = true,
    levels = listOf(Color(0xFF2F5560), Color(0xFF34BFCF), Color(0xFF3F8FC0), Color(0xFF8EA6FF)),
    onLevels = listOf(Color(0xFFD8F3F7), Color(0xFF04232A), Color(0xFF031726), Color(0xFF0B1433)),
    yes = Color(0xFF4FA3D1),
    missing = Color(0xFF4F6374),
    sleepBar = Color(0xFF5F86A6),
    grid = Color(0xFF283038),
    chartText = Color(0xFF9AA4AE),
    chartBackground = Color(0xFF161B21),
    topBar = Color(0xFF14273D),
    onTopBar = Color(0xFFE3EEFA),
)

val LocalMoodColors = staticCompositionLocalOf { DarkMood }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6CB8F6), onPrimary = Color(0xFF00213A),
    primaryContainer = Color(0xFF123E66), onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFF4FC3D7), onSecondary = Color(0xFF00252C),
    secondaryContainer = Color(0xFF1C3550), onSecondaryContainer = Color(0xFFD6E6FF),
    tertiary = Color(0xFF8EA6FF),
    background = Color(0xFF0E1216), onBackground = Color(0xFFE2E6EB),
    surface = Color(0xFF0E1216), onSurface = Color(0xFFE2E6EB),
    surfaceVariant = Color(0xFF232A31), onSurfaceVariant = Color(0xFFA3ADB7),
    surfaceContainerLowest = Color(0xFF0A0D10), surfaceContainerLow = Color(0xFF13181D),
    surfaceContainer = Color(0xFF161B21), surfaceContainerHigh = Color(0xFF1D232A),
    surfaceContainerHighest = Color(0xFF252C34),
    outline = Color(0xFF4A545E), outlineVariant = Color(0xFF2E363E),
    inverseSurface = Color(0xFFE2E6EB), inverseOnSurface = Color(0xFF1A1F24),
)

private val LightScheme = lightColorScheme(
    primary = Brand, onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF), onPrimaryContainer = Color(0xFF0D3A66),
    secondary = Color(0xFF00ACC1),
    secondaryContainer = Color(0xFFD6E9FF), onSecondaryContainer = Color(0xFF0D3A66),
    background = Color.White, surface = Color.White,
    surfaceContainer = Color(0xFFF4F5F6), surfaceContainerHigh = Color(0xFFECEFF1),
)

/** Theme comes from [ThemeSettings] (dark by default), not from the system setting. */
@Composable
fun MindfuckTheme(content: @Composable () -> Unit) {
    val dark = !ThemeSettings.light
    CompositionLocalProvider(LocalMoodColors provides if (dark) DarkMood else LightMood) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = AppTypography, content = content)
    }
}