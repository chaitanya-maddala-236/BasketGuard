package com.basketguard.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BasketColors = lightColorScheme(
    primary = Color(0xFF174B3D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E9DC),
    onPrimaryContainer = Color(0xFF153D32),
    secondary = Color(0xFFEBA05C),
    onSecondary = Color(0xFF2B1B0C),
    background = Color(0xFFF7F6F0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF1E9),
    onSurface = Color(0xFF1E2924),
    onSurfaceVariant = Color(0xFF69756E),
    outline = Color(0xFFDDE3DB),
    error = Color(0xFFB13D32),
)

@Composable
fun BasketGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BasketColors, content = content)
}
