package com.pocketsave.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Minimal theme seed that mirrors iOS `Color.PocketSave.darkPrimary` so the
 * placeholder screens feel like the real app while richer palette/typography
 * work is deferred until later phases (matching the iOS `Config` + `Resources`
 * layers).
 */
private val DarkPrimary = Color(0xFF1F1B1D)
private val Background = Color(0xFFF6F4F5)

private val PocketSaveColors = lightColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    background = Background,
    onBackground = DarkPrimary,
    surface = Background,
    onSurface = DarkPrimary,
)

@Composable
fun PocketSaveTheme(content: @Composable () -> Unit) {
    // Explicitly light to match iOS `preferredColorScheme(.light)`; ignoring system dark mode.
    @Suppress("UNUSED_VARIABLE") val ignored = isSystemInDarkTheme()
    MaterialTheme(colorScheme = PocketSaveColors, content = content)
}
