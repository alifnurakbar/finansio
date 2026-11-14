package com.finansio.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Lime,
    secondary = Lavender,
    tertiary = Yellow,
    error = Coral,
    background = Background,
    surface = Surface,
    onPrimary = Background,
    onSecondary = Background,
    onTertiary = Background,
    onError = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextPrimary
)

/** Tema Material 3 gelap Finansio. */
@Composable
fun FinansioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}