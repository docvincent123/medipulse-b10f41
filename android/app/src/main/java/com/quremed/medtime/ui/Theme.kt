package com.quremed.medtime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MedTimeColors = darkColorScheme(
    primary = Color(0xFF6EE7F9),
    onPrimary = Color(0xFF041017),
    primaryContainer = Color(0xFF123A47),
    onPrimaryContainer = Color(0xFFE7FAFF),
    secondary = Color(0xFF8BA7FF),
    onSecondary = Color(0xFF071017),
    secondaryContainer = Color(0xFF243353),
    onSecondaryContainer = Color(0xFFEDF1FF),
    tertiary = Color(0xFFA98CFF),
    onTertiary = Color(0xFF0E0916),
    tertiaryContainer = Color(0xFF332650),
    onTertiaryContainer = Color(0xFFF2EAFF),
    background = Color(0xFF071017),
    onBackground = Color(0xFFF4F7FA),
    surface = Color(0xFF0D1821),
    onSurface = Color(0xFFF4F7FA),
    surfaceVariant = Color(0xFF14232E),
    onSurfaceVariant = Color(0xFFC5D0DA),
    surfaceContainer = Color(0xFF101D26),
    surfaceContainerHigh = Color(0xFF172630),
    surfaceContainerHighest = Color(0xFF1E2E39),
    outline = Color(0xFF8294A2),
    outlineVariant = Color(0xFF3C4C57),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF320808),
    errorContainer = Color(0xFF5A1E22),
    onErrorContainer = Color(0xFFFFDADB),
    inverseSurface = Color(0xFFE4EBF0),
    inverseOnSurface = Color(0xFF162027),
    inversePrimary = Color(0xFF00677B),
    scrim = Color.Black
)

@Composable
fun MedTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MedTimeColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
