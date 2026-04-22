package com.pocketsave.core.home.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.PocketSaveDimens
import com.pocketsave.common.ui.PocketSaveSprings
import kotlin.math.abs

/**
 * The iOS "side menu" pattern. Not a Material ModalDrawer — it's the 3D page-flip
 * drawer from Core/Home/HomeView.swift:196-219.
 *
 * When open:
 *   - `menuContent` slides in from x = -300 → 0 and rotates its front face to
 *     camera (rotationY: -30° → 0°).
 *   - `homeContent` scales to 0.9, offsets by +265dp, rotates away from camera
 *     (rotationY: 0° → +30°), and its page corners grow from 24 → 30dp.
 *   - Backdrop behind both fades from #212427 (dark) → #F4F4F4 (light).
 *
 * Matches iOS spring(response=0.5, damping=0.7) — PocketSaveSprings.Menu.
 *
 * Swipe gestures:
 *   - Open: horizontal drag rightward from the left edge (detected inside the
 *     binder sidebar area in HomeScreen).
 *   - Close: horizontal drag leftward anywhere on the content shield.
 *
 * @param isOpen one-way bool; callbacks should flip this in the parent state.
 * @param onClose invoked when user swipes or taps the shield to close.
 */
@Composable
fun MenuDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    menuContent: @Composable () -> Unit,
    homeContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = PocketSaveSprings.Menu,
        label = "menuProgress",
    )

    val backdrop by animateColorAsState(
        targetValue = if (isOpen) PocketSaveColors.MenuBackdropOpen else PocketSaveColors.MenuBackdropClosed,
        label = "menuBackdrop",
    )

    val density = LocalDensity.current

    Box(modifier.fillMaxSize().background(backdrop)) {
        // Menu content (left) — rotated away from the camera when hidden.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = with(density) { (-300).dp.toPx() } * (1f - progress)
                    rotationY = -PocketSaveDimens.MenuRotationDegrees * (1f - progress)
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    cameraDistance = 16f * density.density
                    clip = false
                    alpha = progress.coerceAtLeast(0.001f)
                },
        ) {
            menuContent()
        }

        // Home content (right) — scales down and rotates away when the menu opens.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = with(density) { PocketSaveDimens.MenuContentOffsetX.toPx() } * progress
                    val scale = 1f - (1f - PocketSaveDimens.MenuContentScale) * progress
                    translationX = offset
                    scaleX = scale
                    scaleY = scale
                    rotationY = PocketSaveDimens.MenuRotationDegrees * progress
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    cameraDistance = PocketSaveDimens.MenuPerspectiveZ * density.density
                    shape = RoundedCornerShape(
                        PocketSaveDimens.PageCornerRadiusClosed +
                            (PocketSaveDimens.PageCornerRadiusOpen - PocketSaveDimens.PageCornerRadiusClosed) * progress,
                    )
                    clip = true
                },
        ) {
            homeContent()

            // Invisible close shield over the home content when the menu is open.
            // Tapping or leftward-swiping triggers onClose.
            if (isOpen) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {},
                            ) { change, dragAmount ->
                                // Leftward drag > 56dp threshold closes.
                                if (dragAmount < 0 && abs(change.position.x) > 56f) {
                                    onClose()
                                }
                            }
                        },
                )
            }
        }
    }
}
