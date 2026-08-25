package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BossColorScheme = darkColorScheme(
    primary = BossCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = BossCyanDark,
    onPrimaryContainer = Color.White,
    secondary = BossGoldSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF5A4400),
    onSecondaryContainer = BossGoldSecondary,
    tertiary = BossEmerald,
    onTertiary = Color.Black,
    background = BossDarkBg,
    onBackground = BossTextPrimary,
    surface = BossSurface,
    onSurface = BossTextPrimary,
    surfaceVariant = BossSurfaceVariant,
    onSurfaceVariant = BossTextSecondary,
    outline = BossCardBorder,
    error = BossCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BossColorScheme,
        typography = Typography,
        content = content
    )
}
