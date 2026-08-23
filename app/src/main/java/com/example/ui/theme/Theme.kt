package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MTCyan,
    onPrimary = Color.Black,
    primaryContainer = MTCyanDark,
    onPrimaryContainer = Color.White,
    secondary = ZAGold,
    onSecondary = Color.Black,
    secondaryContainer = ZAGoldDark,
    onSecondaryContainer = Color.White,
    tertiary = FileColorApk,
    background = CarbonDark,
    onBackground = Color(0xFFF1F5F9),
    surface = CarbonSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = CarbonSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = CarbonBorder
)

private val LightColorScheme = lightColorScheme(
    primary = MTCyanDark,
    onPrimary = Color.White,
    primaryContainer = MTCyanLight,
    onPrimaryContainer = Color(0xFF003049),
    secondary = ZAGoldDark,
    onSecondary = Color.White,
    secondaryContainer = ZAGoldLight,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = FileColorApk,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek MT-dark theme for power user file management
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
