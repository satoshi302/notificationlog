package com.example.notificationlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Green,
    secondary = GreenDark
)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = GreenDark,
    secondary = Green
)

@Composable
fun NotificationLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
