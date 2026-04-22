package com.pocketsave.core.home.binder

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.PocketSaveDimens
import com.pocketsave.common.ui.components.CartRowGrunge

/**
 * The two pages the binder flips between. iOS reference: HomeBinderPage enum
 * in Core/Home/Views/Binder/HomeBinderPage.swift.
 */
enum class BinderPage { HOME, STATISTICS }

/**
 * Vertical binder sidebar on the left edge of Home. 28dp wide. iOS reference:
 * Core/Home/Views/Binder/Sidebar/HomeBinderSidebarView.swift and subviews.
 *
 * Contains two tappable tabs with labels rotated -90° so they read bottom-up
 * along the page edge:
 *   "Home"             — primary page
 *   "Insights (soon)"  — secondary (statistics) page. iOS labels this with
 *                        a "(soon)" suffix because the full statistics page
 *                        is still in development; we match that copy.
 *
 * The sidebar surface has a faint grunge texture matching the main page
 * surface so they read as a single binder.
 */
@Composable
fun BinderSidebar(
    selected: BinderPage,
    onSelect: (BinderPage) -> Unit,
    modifier: Modifier = Modifier,
    safeAreaTopPadding: androidx.compose.ui.unit.Dp = 47.dp,
) {
    Box(
        modifier
            .width(PocketSaveDimens.BinderWidth)
            .fillMaxHeight()
            .background(PocketSaveColors.DarkPrimary),
    ) {
        // Grunge texture over the binder for paper feel.
        CartRowGrunge(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = safeAreaTopPadding + 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BinderTab(
                label = "Home",
                isSelected = selected == BinderPage.HOME,
                onClick = { onSelect(BinderPage.HOME) },
            )
            BinderTab(
                label = "Insights (soon)",
                isSelected = selected == BinderPage.STATISTICS,
                onClick = { onSelect(BinderPage.STATISTICS) },
            )
            Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun BinderTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = com.pocketsave.core.haptics.rememberAppHaptics()
    val tint by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
        label = "binderTint",
    )

    Box(
        Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, radius = 32.dp, color = Color.White),
                onClick = {
                    if (!isSelected) {
                        haptics.perform(com.pocketsave.core.haptics.AppHaptic.Light)
                    }
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .rotate(-90f)
                .wrapContentSize(unbounded = true),
        )
    }
}
