package com.pocketsave.core.vault

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * "New item flies into the vault list" animation.
 *
 * Motion design (revised for smoothness + delight):
 *   - **One unified progress driver** for position, arc, and scale. Four
 *     independent springs read as jittery; a single emphasized-easing tween
 *     gives a single continuous velocity curve — noticeably smoother to the
 *     eye even though it's technically "less physics-accurate."
 *   - **Long, cinematic timing**: 900 ms flight with Material's emphasized
 *     cubic-bezier easing (`(0.2, 0, 0, 1)`). The ghost has time to be read
 *     as it crosses the screen instead of flashing past.
 *   - **Generous arc**: 110 dp peak (was 72), `sin(πt)^0.8` for a slightly
 *     lazy apex — the ghost clearly rises over the page, not just slides.
 *   - **Lifted card**: 8 dp shadow elevation during travel so the ghost reads
 *     as a physical object in motion.
 *   - **Overlap-fade**: alpha begins tapering in the last 22 % of the flight,
 *     so the ghost seamlessly hands off to the real row instead of landing
 *     opaque and then disappearing.
 *   - **Beefy arrival pulse** on the real row: 1.15× with medium bounce, held
 *     for 500 ms — the "yes, it landed here" beat you should feel, not search
 *     for.
 */

/** Data about an in-flight animation. Immutable so the overlay stays stable. */
@Immutable
internal data class VaultFlight(
    /** The item the user just saved. Used to key the hidden row. */
    val itemId: String,
    val itemName: String,
    val imageUri: String?,
    /** Save-button centre in root coordinates when Save was tapped. */
    val originCenter: Offset,
)

/**
 * Small state holder hoisted by `VaultScreen`. [flight] drives the overlay;
 * [justLandedId] triggers the arrival pulse on the real row for ~500 ms after
 * the flight completes.
 */
internal class VaultFlightState {
    var flight by mutableStateOf<VaultFlight?>(null)
        private set
    var justLandedId by mutableStateOf<String?>(null)
        private set

    fun start(flight: VaultFlight) { this.flight = flight }
    fun clear() { this.flight = null }

    /** Called by [FlightGhost.onFinished]: the ghost has landed. */
    fun markLanded(id: String) {
        flight = null
        justLandedId = id
    }

    fun clearJustLanded() {
        justLandedId = null
    }
}

@Composable
internal fun rememberVaultFlightState(): VaultFlightState = remember { VaultFlightState() }

/**
 * Material-3 emphasized easing. Starts very gently, accelerates smoothly,
 * and decelerates with a long tail into the landing. Gives the ghost the
 * "takes a beat, then glides home" quality that's characteristic of
 * well-polished arrival animations.
 */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * Renders the ghost for [flight] above [listState]'s LazyColumn and animates
 * from `flight.originCenter` to the target row's centre. Calls [onFinished]
 * once the ghost lands so the caller can flip state and reveal the real row.
 *
 * [listRootOrigin] is the LazyColumn's top-left in root coordinates (captured
 * by the caller via `Modifier.onGloballyPositioned`). It's the correct
 * reference frame for translating `listState`'s viewport-local row offsets
 * into root space — using the overlay Box's own origin would land the ghost
 * above the list entirely, because the overlay spans the whole window while
 * the LazyColumn sits below the top bar + search + category strip.
 */
@Composable
internal fun FlightGhost(
    flight: VaultFlight,
    listState: LazyListState,
    listRootOrigin: Offset,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val arcHeightPx = with(density) { ARC_HEIGHT_DP.toPx() }

    // Unified driver — position, arc height, and scale all read off `progress`
    // so their velocity curves are identical. Only alpha moves independently
    // (it needs to overlap with the tail of the flight).
    val progress = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    // Overlay Box's own root-coord origin. Needed to convert the target row's
    // viewport-local offset back into the root space our origin lives in.
    var overlayOrigin by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(flight.itemId) {
        // Lead-in: let the ModalBottomSheet finish sliding away.
        delay(SHEET_DISMISS_LEAD_IN_MS)

        // Fade in so the ghost doesn't pop onto the screen.
        launch { alpha.animateTo(1f, tween(FADE_IN_MS, easing = LinearEasing)) }

        // Run the main flight and the overlap-fade as two parallel coroutines
        // so the alpha starts tapering before the position animation ends —
        // the ghost "dissolves" into its landing slot instead of landing solid
        // and then blinking out.
        coroutineScope {
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(FLIGHT_DURATION_MS, easing = EmphasizedEasing),
                )
            }
            launch {
                // Wait until the flight is 78% done, then begin fading over
                // the remaining visible span (plus a bit beyond).
                delay(OVERLAP_FADE_START_MS)
                alpha.animateTo(0f, tween(FADE_OUT_MS, easing = LinearEasing))
            }
        }
        onFinished()
    }

    // Recompute the target every composition — if the list scrolls or the
    // device rotates mid-flight the ghost chases the correct row.
    val targetCenter = computeTargetCenter(listState, flight.itemId, listRootOrigin)
    val dst = targetCenter ?: flight.originCenter
    val t = progress.value.coerceIn(0f, 1f)
    val x = lerp(flight.originCenter.x, dst.x, t)
    val straightY = lerp(flight.originCenter.y, dst.y, t)
    // Arc: sin(πt)^0.8 lifts the peak slightly later for a lazy, unhurried
    // apex. Arc height is 110 dp — clearly above a straight path.
    val arcBump = sin(t.toDouble() * PI).coerceAtLeast(0.0).pow(0.8).toFloat() * arcHeightPx
    val y = straightY - arcBump
    // Scale settles from full size to 82 % as the ghost approaches the row.
    // A gentle curve, so the size change is barely perceptible in the first
    // half and accelerates as the ghost "commits" to its destination.
    val scale = lerp(1f, 0.82f, t)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() },
    ) {
        val origin = overlayOrigin
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            // 8 dp elevation gives the ghost a clearly "lifted" shadow while it
            // travels — tangible physical-object feel without a double-shadow
            // from an additional Modifier.shadow.
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offsetInRoot(x = x, y = y, overlayOrigin = origin)
                .scale(scale)
                .alpha(alpha.value),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhostThumbnail(uri = flight.imageUri)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = flight.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GhostThumbnail(uri: String?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        } else {
            Text(text = "\uD83D\uDCE6", style = MaterialTheme.typography.titleLarge) // 📦
        }
    }
}

/**
 * Translates the target row's viewport-local offset (from [LazyListState]) into
 * the same root-coord space as [VaultFlight.originCenter]. The key reference
 * point is [listRootOrigin] — the LazyColumn's top-left in root coords — NOT
 * the overlay Box's origin. The overlay spans the whole window; the LazyColumn
 * is nested inside the Scaffold, typically hundreds of pixels below the
 * window's top edge. Returns null while the row isn't yet visible or the
 * LazyColumn hasn't been positioned.
 */
private fun computeTargetCenter(
    listState: LazyListState,
    itemId: String,
    listRootOrigin: Offset,
): Offset? {
    val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == itemId } ?: return null
    val rowCenterY = listRootOrigin.y + info.offset + info.size / 2f
    val viewportWidth = listState.layoutInfo.viewportSize.width
    val rowCenterX = listRootOrigin.x + viewportWidth / 2f
    return Offset(rowCenterX, rowCenterY)
}

/**
 * Positions the ghost so its top-left lands at root coordinates (x, y) minus
 * the overlay's own root origin. A custom [layout] keeps the pixel arithmetic
 * explicit since `Modifier.offset` works in Dp, not raw pixels.
 */
private fun Modifier.offsetInRoot(x: Float, y: Float, overlayOrigin: Offset?): Modifier =
    this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val dx = (x - (overlayOrigin?.x ?: 0f)).roundToInt()
            val dy = (y - (overlayOrigin?.y ?: 0f)).roundToInt()
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(dx, dy)
            }
        },
    )

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

// --- Timing dials --------------------------------------------------------

private val ARC_HEIGHT_DP = 110.dp

/** Wait while the ModalBottomSheet slides off so the ghost isn't covered. */
private const val SHEET_DISMISS_LEAD_IN_MS = 320L

/** Full duration of the position + scale travel. Paced to feel cinematic. */
private const val FLIGHT_DURATION_MS = 900

/** Entry fade so the ghost doesn't pop onto the screen. */
private const val FADE_IN_MS = 180

/**
 * The ghost begins fading this many ms after travel starts — i.e. ~78 % of
 * the way through the 900 ms flight — so alpha finishes around the same time
 * as the position animation. The dissolve-on-arrival read much more smoothly
 * than a hard "land, then fade" sequence.
 */
private const val OVERLAP_FADE_START_MS = 700L

/** Duration of the tail fade. */
private const val FADE_OUT_MS = 220

/** How long the landed row holds its bump state before auto-clearing. */
internal const val LANDED_PULSE_HOLD_MS = 500L
