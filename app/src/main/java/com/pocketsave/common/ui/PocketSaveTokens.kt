package com.pocketsave.common.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

// -----------------------------------------------------------------------------
// iOS-parity color tokens.
//
// Source of truth: Grock-main/PocketSave/Extensions/Color.swift (Color.PocketSave).
// Do not invent new hues — match the iOS hex or derive via darker/lighter/saturated.
// -----------------------------------------------------------------------------
object PocketSaveColors {
    // Brand & status
    val AccentDanger       = Color(0xFFFA003F)
    val AccentBlue         = Color(0xFF278DD9)
    val SubscriptionAccent = Color(0xFF6EBC59)
    val Success            = Color(0xFF4CAF50)

    // Budget bar (3-stop gradient)
    val BudgetSafe     = Color(0xFF98F476) // green
    val BudgetWarning  = Color(0xFFF4B576) // amber
    val BudgetOver     = Color(0xFFF47676) // red

    // Savings / overspend accents
    // SavingsAccent = BudgetSafe.darker(0.4).saturated(0.2). Pre-computed.
    val SavingsAccent   = Color(0xFF5FAD3C)
    val OverspendAccent = Color(0xFFFF5A6B)

    // Text
    val TextPrimary   = Color(0xFF231F30)
    val TextSecondary = Color(0xFF666666)
    val TextMuted     = Color(0xFF999999)
    val TextSubtle    = Color(0xFF717171)
    val TextDeep      = Color(0xFF1E2A36)
    val TextDeepAlt   = Color(0xFF2C3E50)

    // Primary ink — the anchor neutral used for pills, buttons, binder.
    val DarkPrimary = Color(0xFF212427)

    // Neutrals
    val Neutral500   = Color(0xFF888888)
    val Neutral300   = Color(0xFFDDDDDD)
    val BorderSubtle = Color(0xFFF2F2F2)

    // Surfaces
    val SurfaceMuted    = Color(0xFFF7F7F7)
    val SurfaceSoft     = Color(0xFFF9F9F9)
    val SurfaceLight    = Color(0xFFF5F5F5)
    val SurfaceElevated = Color(0xFFEEEEEE)

    // Home background (HomeView.swift:513-529)
    val HomeBaseGrey       = Color(0xFFF8F8F8)
    val HomeGradientStop   = Color(0xFFE2E2E2)
    val MenuBackdropClosed = Color(0xFF212427)
    val MenuBackdropOpen   = Color(0xFFF4F4F4)

    // Cart item status chips (Color.swift:74-82)
    val CartChangedDeep       = Color(0xFF4F00B5)
    val CartChangedBackground = Color(0xFFF8EBFF)
    val CartAddedDeep         = Color(0xFF3A3A3A)
    val CartAddedBackground   = Color(0xFFEFEFEF)
    val CartSkippedDeep       = Color(0xFFD85C2E)
    val CartSkippedBackground = Color(0xFFFFE7D8)
    val CartNewDeep           = Color(0xFFFFB300)
    val CartNewBackground     = Color(0xFFFFF9E6)
}

// -----------------------------------------------------------------------------
// Pastel palette (category tints & cart row backgrounds).
// iOS derives these from stored per-category hex values; we keep a 16-swatch
// palette here for ColorOption to cycle through, matching the iOS feel.
// -----------------------------------------------------------------------------
@Immutable
data class PastelPalette(
    val mintSoft: Color,
    val mintDeep: Color,
    val peachSoft: Color,
    val peachDeep: Color,
    val butterSoft: Color,
    val butterDeep: Color,
    val lavenderSoft: Color,
    val lavenderDeep: Color,
    val blushSoft: Color,
    val blushDeep: Color,
    val skySoft: Color,
    val skyDeep: Color,
    val canvas: Color,
    val canvasTint: Color,
    val hairline: Color,
    val inkBerry: Color,
    val rewardGold: Color,
    val grain: Color,
)

val LightPastels = PastelPalette(
    // Pastel soft/deep pairs — matches the iOS category tint feel.
    mintSoft     = Color(0xFFDCF3DC),
    mintDeep     = Color(0xFF5FAD3C),

    peachSoft    = Color(0xFFFFE5D4),
    peachDeep    = Color(0xFFD85C2E),

    butterSoft   = Color(0xFFFFF4D6),
    butterDeep   = Color(0xFFCC9A1F),

    lavenderSoft = Color(0xFFF0E5FC),
    lavenderDeep = Color(0xFF6B42A6),

    blushSoft    = Color(0xFFFFDCDE),
    blushDeep    = Color(0xFFD14050),

    skySoft      = Color(0xFFDCEAF7),
    skyDeep      = Color(0xFF2E6FA3),

    // Canvas — iOS uses #F8F8F8 (HomeBaseGrey), sheets use pure white.
    canvas     = PocketSaveColors.HomeBaseGrey,
    canvasTint = PocketSaveColors.SurfaceMuted,
    hairline   = PocketSaveColors.BorderSubtle,

    // Editorial ink — iOS DarkPrimary.
    inkBerry   = PocketSaveColors.DarkPrimary,

    rewardGold = Color(0xFFD49A3C),
    grain      = Color(0x0A212427), // sage grey at 4% alpha
)

val LocalPastels = staticCompositionLocalOf { LightPastels }

// -----------------------------------------------------------------------------
// Spacing scale — kept compatible with existing code.
// -----------------------------------------------------------------------------
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

// -----------------------------------------------------------------------------
// Dimension tokens that appear repeatedly in iOS layout.
// -----------------------------------------------------------------------------
object PocketSaveDimens {
    val BinderWidth = 28.dp            // HomeView.swift:11
    val PageCornerRadiusClosed = 24.dp // main content corner when menu closed
    val PageCornerRadiusOpen   = 30.dp // main content corner when menu open
    val SheetCornerRadius      = 28.dp // presentationCornerRadius(28)
    val VaultSheetCornerRadius = 32.dp // HomeView.swift:957
    val CartRowCornerRadius    = 16.dp
    val MenuRowCornerRadius    = 12.dp
    val StickyHeaderHeight     = 134.dp
    val MenuRowHorizontalPadding     = 6.dp
    val MenuListHorizontalPadding    = 12.dp
    val FabDiameter            = 64.dp
    val TabUnderlineHeight     = 2.dp

    // Menu 3D open transform (HomeView.swift:578-583)
    val MenuContentOffsetX = 265.dp
    const val MenuContentScale = 0.9f
    const val MenuRotationDegrees = 30f
    const val MenuPerspectiveZ = 8f
}

// -----------------------------------------------------------------------------
// Pebble / soft card shapes — kept for Android-flavored tiles and legacy code.
// -----------------------------------------------------------------------------
object AppShapes {
    val Pebble: Shape = RoundedCornerShape(
        topStart = 28.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 28.dp,
    )
    val PebbleAlt: Shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 18.dp,
    )
    val Sticker: Shape = RoundedCornerShape(16.dp)
    val SoftCard: Shape = RoundedCornerShape(22.dp)
    val HeroCard: Shape = RoundedCornerShape(28.dp)

    // iOS-matched tokens
    val CartRow: Shape = RoundedCornerShape(PocketSaveDimens.CartRowCornerRadius)
    val MenuRow: Shape = RoundedCornerShape(PocketSaveDimens.MenuRowCornerRadius)
    val Sheet: Shape = RoundedCornerShape(
        topStart = PocketSaveDimens.SheetCornerRadius,
        topEnd = PocketSaveDimens.SheetCornerRadius,
    )
}

object PocketSaveTokens {
    val pastels: PastelPalette
        @Composable @ReadOnlyComposable get() = LocalPastels.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

// -----------------------------------------------------------------------------
// Motion constants. iOS spring presets translated to Compose stiffness/damping.
//
// Mapping: stiffness ≈ (2π / response)² × mass. With mass=1, a response=0.32
// maps to stiffness ≈ 385. We use round numbers that feel right on-device.
// -----------------------------------------------------------------------------
object Motion {
    const val FastMs = 160
    const val MediumMs = 280
    const val SlowMs = 420
    const val EntranceStaggerMs = 70
    const val CelebrationHoldMs = 520

    // iOS easing curves observed in HomeView/Menu
    const val BackdropCrossfadeMs = 280  // .easeInOut(0.28)
    const val PopoverScrimMs = 160       // .easeInOut(0.16)
    const val TabProgressMs = 300        // .easeInOut(0.3)
}

object PocketSaveSprings {
    // iOS: spring(response=0.32, damping=0.86) — tab underline slide
    val Gentle: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 385f)

    // iOS: spring(response=0.5, damping=0.7) — menu open/close, cart row insertion
    val Menu: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 158f)

    // iOS: spring(response=0.28, damping=0.5) — create cart button pulse
    val Pill: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 504f)

    // iOS: spring(response=0.4, damping=0.85) — category manager open
    val Sheet: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 247f)

    // Legacy aliases for code that still references the old names.
    val Bouncy: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 280f)
    val Snap: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 600f)
    val PopBack: SpringSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 320f)
    val Reward: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 180f)

    val BouncySlide: SpringSpec<IntOffset> =
        spring(dampingRatio = 0.78f, stiffness = 320f)
    val GentleSlide: SpringSpec<IntOffset> =
        spring(dampingRatio = 0.9f, stiffness = 300f)
}

// -----------------------------------------------------------------------------
// Shadows & static props.
// -----------------------------------------------------------------------------
val SoftShadowColor = Color(0x14000000)
val CardShadowColor = Color(0x14261A2A)
val HeroShadowColor = Color(0x1F2B2133)

val HairlineWidth = 1.dp
val CardPadding = 18.dp
val SectionGap = 20.dp

// -----------------------------------------------------------------------------
// Color helpers matching iOS UIColor extensions (Color.swift:117-135).
// HSB-space adjustments.
// -----------------------------------------------------------------------------
fun Color.darker(by: Float): Color = adjustBrightness(-by)
fun Color.lighter(by: Float): Color = adjustBrightness(by)

fun Color.saturated(by: Float): Color {
    val hsv = floatArrayOf(0f, 0f, 0f)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsv,
    )
    hsv[1] = (hsv[1] + by).coerceIn(0f, 1f)
    val argb = android.graphics.Color.HSVToColor(hsv)
    return Color(
        red = android.graphics.Color.red(argb) / 255f,
        green = android.graphics.Color.green(argb) / 255f,
        blue = android.graphics.Color.blue(argb) / 255f,
        alpha = alpha,
    )
}

private fun Color.adjustBrightness(delta: Float): Color {
    val hsv = floatArrayOf(0f, 0f, 0f)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsv,
    )
    hsv[2] = (hsv[2] + delta).coerceIn(0f, 1f)
    val argb = android.graphics.Color.HSVToColor(hsv)
    return Color(
        red = android.graphics.Color.red(argb) / 255f,
        green = android.graphics.Color.green(argb) / 255f,
        blue = android.graphics.Color.blue(argb) / 255f,
        alpha = alpha,
    )
}

/**
 * iOS `vaultCategoryAccentGradient(endRadius:)` — a radial gradient from a
 * slightly darker/saturated primary to a deeper, more saturated secondary.
 * See Color.swift:104-114.
 */
fun Color.vaultCategoryAccentGradient(): Brush {
    val primary = darker(0.07f).saturated(0.03f)
    val secondary = darker(0.15f).saturated(0.05f)
    return Brush.radialGradient(
        colors = listOf(primary, secondary),
        radius = 30f,
    )
}
