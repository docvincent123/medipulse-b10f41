package com.quremed.medtime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MedTimeColors = darkColorScheme(
    primary = Color(0xFF6EE7F9),
    onPrimary = Color(0xFF041017),
    secondary = Color(0xFF8BA7FF),
    tertiary = Color(0xFFA98CFF),
    background = Color(0xFF071017),
    surface = Color(0xFF0D1821),
    surfaceVariant = Color(0xFF14232E),
    onBackground = Color(0xFFF4F7FA),
    onSurface = Color(0xFFF4F7FA),
    onSurfaceVariant = Color(0xFFA7B5C3),
    error = Color(0xFFFF8A8A)
)

@Composable
fun MedTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MedTimeColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
