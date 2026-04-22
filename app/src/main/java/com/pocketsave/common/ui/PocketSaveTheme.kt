package com.pocketsave.common.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.R

/**
 * iOS-parity theme.
 *
 * Source of truth: Grock-main/PocketSave/Extensions/Color.swift +
 * Grock-main/PocketSave/Utils/FontManager.swift.
 *
 * DarkPrimary (#212427) is the anchor neutral for pills, headers, binder sidebar,
 * and dark CTAs — not sage. FuzzyBubbles-Bold is the accent font for wordmarks,
 * the big savings number, and cart names.
 */

// MaterialTheme colorScheme — iOS doesn't really use Material semantic slots,
// but we map sensibly so any Material components (TextField, Snackbar, etc.)
// that do consume them stay on-brand.
private val PocketSaveColorScheme = lightColorScheme(
    primary          = PocketSaveColors.DarkPrimary,
    onPrimary        = Color.White,
    primaryContainer = PocketSaveColors.SurfaceElevated,
    onPrimaryContainer = PocketSaveColors.DarkPrimary,

    secondary        = PocketSaveColors.DarkPrimary,
    onSecondary      = Color.White,
    secondaryContainer = PocketSaveColors.SurfaceMuted,
    onSecondaryContainer = PocketSaveColors.DarkPrimary,

    tertiary         = PocketSaveColors.AccentBlue,
    onTertiary       = Color.White,
    tertiaryContainer = Color(0xFFE1F1FF),
    onTertiaryContainer = PocketSaveColors.TextDeep,

    background       = PocketSaveColors.HomeBaseGrey,
    onBackground     = PocketSaveColors.TextPrimary,
    surface          = Color.White,
    onSurface        = PocketSaveColors.TextPrimary,
    surfaceVariant   = PocketSaveColors.SurfaceMuted,
    onSurfaceVariant = PocketSaveColors.TextSecondary,
    surfaceTint      = PocketSaveColors.DarkPrimary,

    outline          = PocketSaveColors.Neutral300,
    outlineVariant   = PocketSaveColors.BorderSubtle,

    error            = PocketSaveColors.AccentDanger,
    onError          = Color.White,
    errorContainer   = Color(0xFFFFE5EA),
    onErrorContainer = PocketSaveColors.AccentDanger,
)

// -----------------------------------------------------------------------------
// Font families.
//
// Poppins is the body family (matches iOS `AppFont.poppins` default).
// FuzzyBubbles is the accent family — iOS uses it for the hand-drawn "vault"
// wordmark, the big savings number, cart names, and primary CTA labels.
// -----------------------------------------------------------------------------
val PoppinsFamily: FontFamily = FontFamily(
    Font(R.font.poppins_light,    FontWeight.Light),
    Font(R.font.poppins_regular,  FontWeight.Normal),
    Font(R.font.poppins_medium,   FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold,     FontWeight.Bold),
)

val FuzzyBubblesFamily: FontFamily = FontFamily(
    Font(R.font.fuzzybubbles_regular, FontWeight.Normal),
    Font(R.font.fuzzybubbles_bold,    FontWeight.Bold),
)

/** Kept as an alias so any existing references to `DisplayFamily` keep compiling. */
val DisplayFamily: FontFamily = PoppinsFamily

/**
 * iOS `Font.accentFont(size:)` — FuzzyBubbles-Bold. Used for:
 *  - Onboarding "PocketSave" wordmark (40sp)
 *  - Primary CTA pill labels ("Get Started", "Try It Now")
 *  - "vault" wordmark (Vault sheet header)
 *  - Home savings big number (48sp)
 *  - Cart names (when user picks FuzzyBubbles in FontManager — default)
 *  - Paywall titles
 */
fun accentTextStyle(size: Int, lineHeight: Int? = null): TextStyle = TextStyle(
    fontFamily = FuzzyBubblesFamily,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = (lineHeight ?: size).sp,
)

// -----------------------------------------------------------------------------
// Typography — matches iOS AppFont sizes (FontManager.swift:118-281)
// with Poppins as the body family.
//
// Material 3 slot → iOS role:
//   displayLarge  ≈ iOS appLargeTitle   (34sp Bold) — paywall hero
//   displayMedium ≈ iOS appTitle        (28sp Bold) — category manager
//   displaySmall  ≈ iOS appTitle2       (22sp SemiBold) — nav bar title
//   headlineLarge ≈ iOS appTitle3       (20sp SemiBold) — sheet titles
//   headlineMedium ≈ iOS appHeadline    (17sp SemiBold) — primary labels
//   headlineSmall  ≈ iOS appHeadline    (17sp SemiBold) — cart row title
//   titleLarge     ≈ appSubheadline+    (18sp Medium)
//   titleMedium    ≈ iOS appSubheadline (15sp Medium)
//   titleSmall     ≈ iOS appFootnote    (13sp Medium)
//   bodyLarge      ≈ iOS appBody        (17sp Regular)
//   bodyMedium     ≈ iOS appCallout     (16sp Regular)
//   bodySmall      ≈ iOS appSubheadline (15sp Regular)
//   labelLarge     ≈ iOS appFootnote    (13sp SemiBold)
//   labelMedium    ≈ iOS appCaption     (12sp Medium)
//   labelSmall     ≈ iOS appCaption2    (11sp Regular)
// -----------------------------------------------------------------------------
private val PocketSaveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.4).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.1).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

private val PocketSaveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PocketSaveTheme(content: @Composable () -> Unit) {
    // iOS hardcodes light mode (GrockApp.swift:485, Info.plist UIUserInterfaceStyle=Light).
    CompositionLocalProvider(
        LocalPastels provides LightPastels,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = PocketSaveColorScheme,
            typography = PocketSaveTypography,
            shapes = PocketSaveShapes,
            content = content,
        )
    }
}
