package com.pocketsave.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Calm muted-sage palette. One accent family for the whole app — buttons,
// selected nav tab, progress, active states, positive indicators all share
// the same ramp. Over-budget / destructive affordances use a single muted
// clay so warnings stay legible without inventing a second brand hue.
private val SagePrimary = Color(0xFF6C9A8B)          // sage 500 — brand accent
private val SagePressed = Color(0xFF5E8F7B)          // sage 600 — hover/pressed
private val SageContainer = Color(0xFFE2EDE6)        // sage 100
private val OnSageContainer = Color(0xFF2D4E3D)      // sage 800

private val ClayWarning = Color(0xFFB5734A)          // muted clay (semantic warn)
private val ClayContainer = Color(0xFFF3E3D8)        // clay 100
private val OnClayContainer = Color(0xFF7A3D1B)

private val Ink900 = Color(0xFF1C2A20)               // primary text
private val Ink700 = Color(0xFF44574C)               // secondary text (not used directly — M3 picks onSurfaceVariant)
private val Ink500 = Color(0xFF6D7C72)               // tertiary / muted text

private val Canvas = Color(0xFFF7F9F7)               // page bg
private val CanvasMuted = Color(0xFFECF1EE)          // muted surface / surfaceVariant
private val SurfaceWhite = Color(0xFFFFFFFF)
private val Hairline = Color(0xFFE4EAE6)
private val HairlineSoft = Color(0xFFEEF2EF)
private val RoseError = Color(0xFFB5534C)

private val PocketSaveColors = lightColorScheme(
    primary = SagePrimary,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = OnSageContainer,
    // Keep the secondary slot in the sage family too — deeper sage reads as
    // "emphasis" without introducing a second hue.
    secondary = SagePressed,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    // Tertiary reserved for the subtle clay warning so the framework's error
    // slot can stay for true destructive state only.
    tertiary = ClayWarning,
    onTertiary = Color.White,
    tertiaryContainer = ClayContainer,
    onTertiaryContainer = OnClayContainer,
    background = Canvas,
    onBackground = Ink900,
    surface = SurfaceWhite,
    onSurface = Ink900,
    surfaceVariant = CanvasMuted,
    onSurfaceVariant = Ink500,
    surfaceTint = SagePrimary,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    error = RoseError,
    onError = Color.White,
    errorContainer = Color(0xFFFAE4E2),
    onErrorContainer = Color(0xFF6E2622),
)

// System sans-serif with weight/tracking hierarchy — no font dependency.
private val Sans = FontFamily.SansSerif

private val PocketSaveTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-0.6).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 0.05.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.25.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

private val PocketSaveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PocketSaveTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val ignored = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalPastels provides LightPastels,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = PocketSaveColors,
            typography = PocketSaveTypography,
            shapes = PocketSaveShapes,
            content = content,
        )
    }
}
