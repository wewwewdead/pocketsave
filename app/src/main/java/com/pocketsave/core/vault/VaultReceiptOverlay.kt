package com.pocketsave.core.vault

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pocketsave.common.ui.FuzzyBubblesFamily
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.components.DashedLine
import com.pocketsave.common.ui.components.DottedLeader
import com.pocketsave.common.ui.components.ReceiptGrunge
import com.pocketsave.common.ui.shapes.ReceiptTicketShape
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.data.local.entity.ItemEntity
import kotlinx.coroutines.delay

/**
 * Paper-receipt item-detail overlay. Port of iOS
 * Core/Vault/Views/VaultReceiptOverlayView.swift — the signature visual
 * moment when tapping an item: a torn-paper receipt animates up from the
 * bottom, settles near the top, and shows the item's details with a piece
 * of masking tape attached at the top.
 *
 * Simplified vs iOS:
 *   - No CoreHaptics continuous haptic; we fire one heavy impact when the
 *     receipt settles.
 *   - No masking-tape image yet — placeholder rectangle stands in.
 *
 * Full iOS choreography (lift → settle → stickerPop → tapeReveal → topBar):
 *   1. 0.00s  background fades to white
 *   2. 0.00s  receipt slides up from the bottom
 *   3. 0.75s  heavy haptic + receipt settles near top with spring bounce
 *   4. 1.05s  item sticker thumbnail pops in on top
 *   5. 1.35s  tape strip fades in, rotates slightly for hand-stuck feel
 *   6. 1.78s  close button + header fade in
 */
@Composable
fun VaultReceiptOverlay(
    item: ItemEntity,
    storeName: String?,
    priceLabel: String?,
    unitLabel: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        VaultReceiptOverlayContent(
            item = item,
            storeName = storeName,
            priceLabel = priceLabel,
            unitLabel = unitLabel,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun VaultReceiptOverlayContent(
    item: ItemEntity,
    storeName: String?,
    priceLabel: String?,
    unitLabel: String?,
    onDismiss: () -> Unit,
) {
    val haptics = rememberAppHaptics()

    // Multi-stage animation sequence — matches iOS timing beats.
    var stage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Stage 1 — white scrim + receipt begins sliding up.
        delay(10)
        stage = 1
        // Stage 2 — heavy haptic + receipt settles near top.
        delay(750)
        haptics.perform(AppHaptic.Confirm)
        stage = 2
        // Stage 3 — sticker thumbnail pops.
        delay(300)
        stage = 3
        // Stage 4 — tape reveals.
        delay(300)
        stage = 4
        // Stage 5 — top bar reveals.
        delay(430)
        stage = 5
    }

    val blurOpacity by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "blurOpacity",
    )

    val receiptOffsetY by animateDpAsState(
        targetValue = when (stage) {
            0 -> 900.dp
            1 -> 540.dp
            else -> 56.dp
        },
        animationSpec = when (stage) {
            2, 3, 4, 5 -> spring(dampingRatio = 0.5f, stiffness = 180f)
            else -> tween(durationMillis = 750, easing = FastOutSlowInEasing)
        },
        label = "receiptOffsetY",
    )

    val stickerScale by animateFloatAsState(
        targetValue = when (stage) {
            0, 1, 2 -> 0.7f
            3 -> 1.05f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "stickerScale",
    )

    val stickerOpacity by animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "stickerOpacity",
    )

    val tapeOpacity by animateFloatAsState(
        targetValue = if (stage >= 4) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "tapeOpacity",
    )

    val tapeRotation by animateFloatAsState(
        targetValue = if (stage >= 4) -3f else -15f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "tapeRotation",
    )

    val topBarOpacity by animateFloatAsState(
        targetValue = if (stage >= 5) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "topBarOpacity",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = blurOpacity)),
    ) {
        // Close button in top-right.
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close",
            tint = PocketSaveColors.DarkPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 20.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(PocketSaveColors.SurfaceElevated)
                .clickable { onDismiss() }
                .padding(8.dp)
                .graphicsLayer { alpha = topBarOpacity },
        )

        // Receipt paper card.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer { translationY = receiptOffsetY.toPx() }
                .align(Alignment.TopCenter),
        ) {
            // Masking tape strip — paper accent at top.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = tapeOpacity }
                    .rotate(tapeRotation)
                    .width(80.dp)
                    .height(22.dp)
                    .background(Color(0xFFFFE79C).copy(alpha = 0.85f))
                    .padding(1.dp),
            )

            // Receipt card body.
            val ticketShape = remember { ReceiptTicketShape(notchRadius = 6.dp, flatWidth = 5.dp) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(ticketShape)
                    .background(Color.White),
            ) {
                ReceiptGrunge(Modifier.fillMaxSize())

                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Sticker thumbnail.
                    Box(
                        Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = stickerScale
                                scaleY = stickerScale
                                alpha = stickerOpacity
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.imageUri != null) {
                            AsyncImage(
                                model = item.imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PocketSaveColors.SurfaceMuted),
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PocketSaveColors.SurfaceMuted),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "📦",
                                    fontSize = 32.sp,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Item name — FuzzyBubbles hand-drawn headline.
                    Text(
                        text = item.name,
                        fontFamily = FuzzyBubblesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = PocketSaveColors.DarkPrimary,
                    )

                    Spacer(Modifier.height(4.dp))

                    // Store label under name.
                    if (storeName != null) {
                        Text(
                            text = storeName,
                            fontSize = 13.sp,
                            color = PocketSaveColors.TextMuted,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    DashedLine()
                    Spacer(Modifier.height(12.dp))

                    // Receipt rows.
                    ReceiptRow(
                        label = "Price",
                        value = priceLabel ?: "—",
                    )
                    Spacer(Modifier.height(8.dp))
                    ReceiptRow(
                        label = "Unit",
                        value = unitLabel ?: "—",
                    )

                    Spacer(Modifier.height(20.dp))
                    DashedLine()
                    Spacer(Modifier.height(16.dp))

                    // Footer marker.
                    Text(
                        text = "PocketSave",
                        fontFamily = FuzzyBubblesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PocketSaveColors.DarkPrimary.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = PocketSaveColors.TextSecondary,
        )
        DottedLeader(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PocketSaveColors.DarkPrimary,
        )
    }
}
