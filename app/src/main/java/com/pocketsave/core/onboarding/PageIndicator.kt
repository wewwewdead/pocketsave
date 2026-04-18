package com.pocketsave.core.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement

/**
 * Tiny dot indicator for the two middle steps, mirroring the iOS
 * `PageIndicator` companion shown in `OnboardingContainer.swift`.
 */
@Composable
fun PageIndicator(currentStep: OnboardingStep) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = Color(0x33000000)
    val firstActive = currentStep == OnboardingStep.LAST_STORE
    val secondActive = currentStep == OnboardingStep.FIRST_ITEM

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Dot(active = firstActive, activeColor = activeColor, inactiveColor = inactiveColor)
        Dot(active = secondActive, activeColor = activeColor, inactiveColor = inactiveColor)
    }
}

@Composable
private fun Dot(active: Boolean, activeColor: Color, inactiveColor: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (active) activeColor else inactiveColor),
    )
}
