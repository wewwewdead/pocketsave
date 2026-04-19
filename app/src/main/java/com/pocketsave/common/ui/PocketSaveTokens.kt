package com.pocketsave.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// One accent family. Every tint used across the home surface is a different
// step along the sage ramp — the pastel variety pack is gone so the page reads
// as one cohesive system. A single muted-clay token is reserved for "over
// budget" and reset-app warnings; it's intentionally narrow in scope so the
// sage stays the dominant brand note.
@Immutable
data class PastelPalette(
    // Sage tonal ramp (light → dark). Named "mint*" / "peach*" on the data
    // class for backwards compatibility with component code — every field now
    // resolves to a sage tonal or neutral, not a separate hue.
    val mintSoft: Color,       // sage 50  — softest chip background
    val mintDeep: Color,       // sage 700 — ink on sage tints
    val peachSoft: Color,      // sage 100 — next-step chip background
    val peachDeep: Color,      // sage 700 — ink on sage tints
    val butterSoft: Color,     // sage 150 — alt chip background
    val butterDeep: Color,     // sage 600 — ink
    val lavenderSoft: Color,   // sage 200 — strongest tonal chip
    val lavenderDeep: Color,   // sage 800 — strongest ink
    val blushSoft: Color,      // muted clay 100 — semantic warning only
    val blushDeep: Color,      // muted clay 700 — warning ink / reset text
    val skySoft: Color,        // sage 50 (alias)
    val skyDeep: Color,        // sage 700 (alias)
    val canvas: Color,         // page background
    val canvasTint: Color,     // section / muted surface
    val hairline: Color,       // soft divider / outline
)

// Sage-only palette. The named fields above map into the ramp below so
// downstream components (summary pills, quick actions, chips) automatically
// read as one family without rewriting every site.
val LightPastels = PastelPalette(
    mintSoft = Color(0xFFEFF5F1),      // sage 50
    mintDeep = Color(0xFF3F6351),      // sage 700
    peachSoft = Color(0xFFE2EDE6),     // sage 100
    peachDeep = Color(0xFF3F6351),     // sage 700
    butterSoft = Color(0xFFE8F0EA),    // sage 75
    butterDeep = Color(0xFF4C735F),    // sage 650
    lavenderSoft = Color(0xFFD6E6DB),  // sage 200
    lavenderDeep = Color(0xFF2D4E3D),  // sage 800
    blushSoft = Color(0xFFF3E3D8),     // muted clay 100 (warning only)
    blushDeep = Color(0xFF8A4E35),     // muted clay 700
    skySoft = Color(0xFFEFF5F1),
    skyDeep = Color(0xFF3F6351),
    canvas = Color(0xFFF7F9F7),
    canvasTint = Color(0xFFECF1EE),
    hairline = Color(0xFFE4EAE6),
)

val LocalPastels = staticCompositionLocalOf { LightPastels }

// Spacing scale — 4dp base.
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

object PocketSaveTokens {
    val pastels: PastelPalette
        @Composable @ReadOnlyComposable get() = LocalPastels.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

// Motion constants shared by every section so entrance and press feedback
// feel like one system. The named specs below encode Duolingo-flavored
// physics — bouncy when things arrive, firm when they react, pop-back on
// release — so screens, tiles, chips, and the nav bar all move with the
// same playful rhythm.
object Motion {
    const val FastMs = 180
    const val MediumMs = 280
    const val SlowMs = 420
    // Row stagger delay. Bumped up from 60 → 70 so the cascade reads
    // clearly on fast devices without feeling slow on slow ones.
    const val EntranceStaggerMs = 70
}

/**
 * Typed spring specs pre-baked for common motion roles. These exist so
 * callers don't have to hand-pick damping/stiffness pairs — picking the
 * right role keeps the app's motion language consistent.
 *
 *  - [Bouncy]   — things arriving: entrance scale, nav-tab icon hop
 *  - [Gentle]   — things relaxing to rest: progress bars, continuous values
 *  - [Snap]     — things reacting firmly: press-down, quick state flips
 *  - [PopBack]  — things rebounding after release: press-up overshoot
 */
object PocketSaveSprings {
    // --- Float specs (scale, alpha, progress) ----------------------------
    val Bouncy: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 280f)

    val Gentle: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 280f)

    val Snap: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 600f)

    val PopBack: androidx.compose.animation.core.SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 320f)

    // --- IntOffset specs (slide animations) -----------------------------
    val BouncySlide: androidx.compose.animation.core.SpringSpec<androidx.compose.ui.unit.IntOffset> =
        androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 320f)

    val GentleSlide: androidx.compose.animation.core.SpringSpec<androidx.compose.ui.unit.IntOffset> =
        androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 300f)
}

// Card / chip shadow tints.
val SoftShadowColor = Color(0x14000000)
val CardShadowColor = Color(0x141F2A22)

val HairlineWidth = 1.dp
val CardPadding = 18.dp
val SectionGap = 20.dp
