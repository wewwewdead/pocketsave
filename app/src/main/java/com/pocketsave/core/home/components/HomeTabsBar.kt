package com.pocketsave.core.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.PocketSaveSprings

enum class HomeTab { ACTIVE, HISTORY }

/**
 * Active / History tab bar. iOS reference:
 * Core/Home/Views/HomeTabsBarView.swift.
 *
 * Visual:
 *   Active    History                            [trailingTitle, e.g. "2026"]
 *   ───
 *
 * Selected label is DarkPrimary SemiBold. Unselected is DarkPrimary 40%-alpha
 * Medium. A 2dp underline slides between them using PocketSaveSprings.Gentle.
 * Trailing title (small year label) appears only when History tab is active.
 */
@Composable
fun HomeTabsBar(
    selected: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    trailingTitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var activeSize by remember { mutableStateOf(IntSize.Zero) }
    var activeX by remember { mutableStateOf(0) }
    var historySize by remember { mutableStateOf(IntSize.Zero) }
    var historyX by remember { mutableStateOf(0) }

    val targetX = if (selected == HomeTab.ACTIVE) activeX else historyX
    val targetW = if (selected == HomeTab.ACTIVE) activeSize.width else historySize.width

    val underlineX = remember { Animatable(targetX.toFloat()) }
    val underlineW = remember { Animatable(targetW.toFloat()) }

    LaunchedEffect(targetX, targetW) {
        underlineX.animateTo(targetX.toFloat(), animationSpec = PocketSaveSprings.Gentle)
        underlineW.animateTo(targetW.toFloat(), animationSpec = PocketSaveSprings.Gentle)
    }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(Modifier.padding(end = 24.dp)) {
            TabLabel(
                label = "Active",
                isSelected = selected == HomeTab.ACTIVE,
                onClick = { onTabSelected(HomeTab.ACTIVE) },
                onPositioned = { size, x ->
                    activeSize = size
                    activeX = x
                },
            )
        }
        Box {
            TabLabel(
                label = "History",
                isSelected = selected == HomeTab.HISTORY,
                onClick = { onTabSelected(HomeTab.HISTORY) },
                onPositioned = { size, x ->
                    historySize = size
                    historyX = x
                },
            )
        }
        Spacer(Modifier.weight(1f))
        if (trailingTitle != null) {
            Text(
                text = trailingTitle,
                color = PocketSaveColors.DarkPrimary.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    // Underline — drawn as a separate Row below the tabs, position tracks
    // the selected tab's absolute x within the bar.
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(2.dp)
            .drawBehind {
                drawRect(
                    color = PocketSaveColors.DarkPrimary,
                    topLeft = androidx.compose.ui.geometry.Offset(underlineX.value, 0f),
                    size = androidx.compose.ui.geometry.Size(
                        width = underlineW.value.coerceAtLeast(0f),
                        height = size.height,
                    ),
                )
            },
    )
}

@Composable
private fun TabLabel(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPositioned: (IntSize, Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = com.pocketsave.core.haptics.rememberAppHaptics()
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.4f,
        label = "tabAlpha",
    )
    Text(
        text = label,
        color = PocketSaveColors.DarkPrimary.copy(alpha = alpha),
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        fontSize = 15.sp,
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val parent = coords.parentCoordinates
                val xInParent = if (parent != null) {
                    parent.localPositionOf(coords, androidx.compose.ui.geometry.Offset.Zero).x.toInt()
                } else coords.positionInRoot().x.toInt()
                onPositioned(coords.size, xInParent)
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isSelected) {
                        haptics.perform(com.pocketsave.core.haptics.AppHaptic.Light)
                    }
                    onClick()
                },
            ),
    )
}

