package com.pocketsave.core.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.R
import com.pocketsave.common.ui.FuzzyBubblesFamily
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics

/**
 * iOS-parity Welcome screen. Source: Core/Onboarding/Views/OnboardingWelcomeView.swift.
 *
 * Layout:
 *   Spacer
 *   pocketsave_logo (80×80)
 *   PocketSave       (40sp FuzzyBubbles-Bold)
 *   Spacer(60dp)
 *   ⟢   see your true costs           ⟣
 *   ⟢   stop leaks, save more         ⟣
 *   ⟢   forget paper & spreadsheets ! ⟣
 *   ⟢   PLAN & SHOP SMARTER           ⟣
 *   Spacer
 *   [Get Started] — black capsule CTA with gradient
 */
@Composable
fun OnboardingWelcomeScreen(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.pocketsave_logo),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "PocketSave",
            fontFamily = FuzzyBubblesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            color = PocketSaveColors.DarkPrimary,
        )

        Spacer(Modifier.height(60.dp))

        // iOS bullet glyphs — ⟢ U+27E2 / ⟣ U+27E3.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WelcomeBullet("see your true costs")
            WelcomeBullet("stop leaks, save more")
            WelcomeBullet("forget paper & spreadsheets !")
            WelcomeBullet("PLAN & SHOP SMARTER")
        }

        Spacer(Modifier.weight(1f))

        GetStartedButton(
            onClick = { viewModel.navigateToStore() },
        )

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun WelcomeBullet(text: String) {
    Text(
        text = "⟢   $text   ⟣",
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        color = PocketSaveColors.DarkPrimary.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GetStartedButton(onClick: () -> Unit) {
    val haptics = rememberAppHaptics()
    // iOS: Capsule with dark gradient fill, FuzzyBubbles-Bold 16sp white label.
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PocketSaveColors.DarkPrimary.copy(alpha = 0.9f),
                        PocketSaveColors.DarkPrimary,
                    ),
                ),
            )
            .clickable {
                haptics.perform(AppHaptic.Light)
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Get Started",
            fontFamily = FuzzyBubblesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}
