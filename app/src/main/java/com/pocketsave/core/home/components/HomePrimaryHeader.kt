package com.pocketsave.core.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pocketsave.R
import com.pocketsave.common.ui.FuzzyBubblesFamily
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.components.CharacterReveal
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics

/**
 * The hand-drawn Home header showing total net savings. iOS reference:
 * Core/Home/Views/Header/HomePrimaryHeaderView.swift +
 * HomePrimaryHeaderSavingsView.swift.
 *
 * Visual:
 *   [⟳ vault-icon top-right]
 *   {+|-|} {amount}  savings  🔥  (if positive > $0.01)
 *
 * Typography: the huge amount is FuzzyBubbles-Bold at 48sp — the single biggest
 * visual moment in the app. Sign + context are Poppins 15sp muted.
 *
 * Colors:
 *   - amount green (SavingsAccent) if net > $0.01
 *   - amount red (OverspendAccent) if net < -$0.01
 *   - amount 88% DarkPrimary if zero
 *
 * The Fire Lottie plays next to the amount when savings are positive.
 */
@Composable
fun HomePrimaryHeader(
    netSavings: Double,
    currencyFormatter: (Double) -> String,
    safeAreaTopPadding: androidx.compose.ui.unit.Dp = 47.dp,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPositive = netSavings > 0.01
    val isNegative = netSavings < -0.01

    val sign = when {
        isPositive -> "+"
        isNegative -> "-"
        else -> ""
    }
    val amount = currencyFormatter(kotlin.math.abs(netSavings))
    val context = if (isNegative) "overspent" else "savings"
    val accent = when {
        isPositive -> PocketSaveColors.SavingsAccent
        isNegative -> PocketSaveColors.OverspendAccent
        else -> PocketSaveColors.DarkPrimary.copy(alpha = 0.88f)
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(top = safeAreaTopPadding, bottom = 8.dp),
    ) {
        // Top toolbar — vault button on the right.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VaultToolbarButton(onClick = onOpenVault)
        }

        // Savings hand-drawn display. Per-character reveal matches iOS
        // CharacterRevealView (Components/CharacterRevealView.swift).
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CharacterReveal(
                text = sign + amount,
                fontSize = 48.sp,
                color = accent,
                fontFamily = FuzzyBubblesFamily,
                fontWeight = FontWeight.Bold,
                showsUnderline = false,
                staggerMs = 30,
                animateOnChange = true,
            )

            // Context label aligned to baseline of amount
            Text(
                text = context,
                color = PocketSaveColors.TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            if (isPositive) {
                FireLottie(
                    modifier = Modifier
                        .size(30.dp)
                        .padding(bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun VaultToolbarButton(onClick: () -> Unit) {
    val haptics = rememberAppHaptics()
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable {
                haptics.perform(AppHaptic.Light)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = "Open vault",
            tint = PocketSaveColors.DarkPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FireLottie(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_fire))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier,
    )
}
