package com.pocketsave.common.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The PocketSave visual identity lives in this file. Sage remains the brand
 * spine — every call-to-action, selected state, and progress line is sage —
 * but we layer in a small editorial cast of supporting hues so the page reads
 * like a thoughtfully-styled cookbook instead of a monotone utility.
 *
 * Every supporting hue has a muted, slightly warm character: apricot for
 * reward, butter for highlight, lavender-dusk for depth, blush-clay for
 * gentle-warning, sky for calm-stat. No hue is loud; the canvas is linen.
 *
 * Field names are kept from the earlier sage-only era so every downstream
 * component picks up the new identity for free.
 */
@Immutable
data class PastelPalette(
    // Mint — fresh herbaceous green. Reads "in progress / active".
    val mintSoft: Color,
    val mintDeep: Color,
    // Peach / apricot — warm reward note. Reads "earned, on track".
    val peachSoft: Color,
    val peachDeep: Color,
    // Butter — honeyed highlight. Reads "remembered / sparkles".
    val butterSoft: Color,
    val butterDeep: Color,
    // Lavender-dusk — editorial depth. Reads "saved / collected".
    val lavenderSoft: Color,
    val lavenderDeep: Color,
    // Blush-clay — gentle warning. Reads "over budget / needs attention".
    val blushSoft: Color,
    val blushDeep: Color,
    // Sky — cool calm stat. Reads "monthly / info".
    val skySoft: Color,
    val skyDeep: Color,
    // Canvas — page background (warm linen).
    val canvas: Color,
    val canvasTint: Color,
    val hairline: Color,
    // Editorial ink — deep espresso-plum reserved for display headlines
    // and hero wordmarks where the body ink feels too ordinary.
    val inkBerry: Color,
    // Reward gold — rare accent for celebration moments only. Never chrome.
    val rewardGold: Color,
    // Grain dot tint for decor overlays.
    val grain: Color,
)

// Monotone sage palette. Every "supporting-hue" slot resolves to a step
// along the sage ramp so the whole app reads as a single cohesive family.
// The one exception is the blush slot — a reserved muted clay used ONLY
// for over-budget / destructive-warning semantics, so warnings stay legible
// without inventing a second brand hue.
val LightPastels = PastelPalette(
    // sage 100 / sage 700 — the primary tinted card note
    mintSoft = Color(0xFFE2EDE6),
    mintDeep = Color(0xFF3F6351),

    // sage 75 / sage 700 — softer alternate step
    peachSoft = Color(0xFFE8F0EA),
    peachDeep = Color(0xFF3F6351),

    // sage 50 / sage 650 — lightest step + muted ink
    butterSoft = Color(0xFFEFF5F1),
    butterDeep = Color(0xFF4C735F),

    // sage 200 / sage 800 — deepest tinted step + strongest ink
    lavenderSoft = Color(0xFFD6E6DB),
    lavenderDeep = Color(0xFF2D4E3D),

    // muted clay — preserved as the SINGLE non-sage token, reserved
    // exclusively for over-budget nudges and reset-app warnings.
    blushSoft = Color(0xFFF3E3D8),
    blushDeep = Color(0xFF8A4E35),

    // sage 50 / sage 650 — calm stat variant (alias of butter tones)
    skySoft = Color(0xFFEFF5F1),
    skyDeep = Color(0xFF4C735F),

    // Warm linen canvas + cream tint + soft warm hairline.
    canvas = Color(0xFFF8F5EE),
    canvasTint = Color(0xFFEFE9DC),
    hairline = Color(0xFFE7DFD0),

    // Editorial ink — sage-adjacent deep plum-espresso for display copy.
    inkBerry = Color(0xFF22281F),
    // Reward gold kept for the rare celebration confetti accent only.
    // Never used as a card background.
    rewardGold = Color(0xFFD49A3C),
    // Grain uses a sage-ink tint so paper texture stays on-brand.
    grain = Color(0x0A2D4E3D),
)

val LocalPastels = staticCompositionLocalOf { LightPastels }

// Spacing scale — 4dp base, unchanged.
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

/**
 * Characterful shapes. The app's default `Shapes` stay rectilinear-with-rounded
 * corners (Material expects that). These extras are the ones you reach for
 * when a tile should feel like a pebble picked up from a beach — pleasantly
 * asymmetric, never haphazard.
 */
object AppShapes {
    /** Diagonal pebble: big top-left / bottom-right, small top-right / bottom-left. */
    val Pebble: Shape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 28.dp,
    )

    /** Opposite-diagonal pebble — use to alternate rhythm in a row of pebbles. */
    val PebbleAlt: Shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 28.dp,
        bottomStart = 28.dp,
        bottomEnd = 18.dp,
    )

    /** Sticker — a flatter squircle feel for chips and icon badges. */
    val Sticker: Shape = RoundedCornerShape(16.dp)

    /** Soft card — slightly rounder than Material medium. */
    val SoftCard: Shape = RoundedCornerShape(22.dp)

    /** Hero card — editorial proportions. */
    val HeroCard: Shape = RoundedCornerShape(28.dp)
}

object PocketSaveTokens {
    val pastels: PastelPalette
        @Composable @ReadOnlyComposable get() = LocalPastels.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

/**
 * Motion constants shared by every section so entrance and press feedback
 * feel like one system. The specs below encode Duolingo-flavored physics —
 * bouncy on arrival, firm on reaction, pop-back on release.
 */
object Motion {
    const val FastMs = 180
    const val MediumMs = 280
    const val SlowMs = 420
    const val EntranceStaggerMs = 70
    /** Celebration pulse hold before settle — long enough to register in
     *  peripheral vision, short enough not to feel indulgent. */
    const val CelebrationHoldMs = 520
}

/**
 * Typed spring specs pre-baked for common motion roles.
 *
 *  - [Bouncy]   — things arriving: entrance scale, nav-tab icon hop
 *  - [Gentle]   — things relaxing to rest: progress bars, continuous values
 *  - [Snap]     — things reacting firmly: press-down, quick state flips
 *  - [PopBack]  — things rebounding after release: press-up overshoot
 *  - [Reward]   — slow, generous overshoot for celebratory moments
 */
object PocketSaveSprings {
    val Bouncy: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 280f)

    val Gentle: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 280f)

    val Snap: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 600f)

    val PopBack: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 320f)

    val Reward: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 180f)

    val BouncySlide: androidx.compose.animation.core.SpringSpec<androidx.compose.ui.unit.IntOffset> =
        androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 320f)

    val GentleSlide: androidx.compose.animation.core.SpringSpec<androidx.compose.ui.unit.IntOffset> =
        androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 300f)
}

// Shadow tints.
val SoftShadowColor = Color(0x14000000)
val CardShadowColor = Color(0x14261A2A)
val HeroShadowColor = Color(0x1F2B2133)

val HairlineWidth = 1.dp
val CardPadding = 18.dp
val SectionGap = 20.dp
