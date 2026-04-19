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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.R

/**
 * Brand colors. Sage remains the one CTA family. Clay (`blushDeep`) is
 * reserved for over-budget / destructive semantics only.
 */
private val SagePrimary = Color(0xFF6C9A8B)          // sage 500
private val SagePressed = Color(0xFF5E8F7B)          // sage 600
private val SageContainer = Color(0xFFE2EDE6)        // sage 100
private val OnSageContainer = Color(0xFF2D4E3D)      // sage 800

private val ClayWarning = Color(0xFFB5734A)          // muted clay
private val ClayContainer = Color(0xFFF3E3D8)        // clay 100
private val OnClayContainer = Color(0xFF7A3D1B)

private val Ink900 = Color(0xFF22281F)               // primary text (warm espresso-sage)
private val Ink500 = Color(0xFF6B6F63)               // muted body

private val Canvas = Color(0xFFF8F5EE)               // warm linen page bg
private val CanvasMuted = Color(0xFFEFE9DC)          // cream surface variant
private val SurfaceWhite = Color(0xFFFFFFFF)
private val Hairline = Color(0xFFE7DFD0)
private val HairlineSoft = Color(0xFFEFE9DC)
private val RoseError = Color(0xFFB5534C)

private val PocketSaveColors = lightColorScheme(
    primary = SagePrimary,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = OnSageContainer,
    secondary = SagePressed,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
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

/**
 * Poppins — the app's one type family. Bundled as five upright weights
 * under `res/font/`; the framework resolves each style's `FontWeight` to
 * the matching face automatically. Italic requests fall through to a
 * synthesised slant since Poppins-Italic isn't bundled — acceptable for
 * the handful of kicker/caption lines that use italic.
 *
 * Weights shipped:
 *   Light 300, Regular 400, Medium 500, SemiBold 600, Bold 700.
 */
val PoppinsFamily: FontFamily = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/** Kept as a named export for display-role text styles. Points at Poppins
 *  now that the type system has a single brand family. */
val DisplayFamily: FontFamily = PoppinsFamily

/**
 * Typography. All styles use Poppins. Display/headline run heavier with
 * tighter letterspacing for editorial weight; body/label stay calm. Italic
 * is used sparingly on kickers — Android synthesises the slant since
 * Poppins-Italic isn't bundled.
 */
private val PocketSaveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.25.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.7.sp,
    ),
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
