package com.pocketsave.core.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketsave.core.onboarding.motion.onboardingCelebrationPulse
import kotlinx.coroutines.delay

/**
 * Brief handoff moment shown between the Trip step and Home. The container's
 * [androidx.compose.runtime.LaunchedEffect] on `onboardingComplete` fires
 * `onComplete()` the instant the ViewModel flips the flag — Home mounts
 * underneath, and this screen fades out through the step transition as the
 * nav host pops the Onboarding destination.
 *
 * The check mark pulses once via [onboardingCelebrationPulse] so the
 * transition lands with a quiet "done" beat.
 */
@Composable
fun OnboardingHandoffScreen(viewModel: OnboardingViewModel) {
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(40)
        appear = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = appear,
                enter = fadeIn(tween(320)),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .onboardingCelebrationPulse(viewModel.tripCelebrationTrigger),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(
                visible = appear,
                enter = fadeIn(tween(360, delayMillis = 80)),
            ) {
                Text(
                    text = "You're set.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(6.dp))

            AnimatedVisibility(
                visible = appear,
                enter = fadeIn(tween(360, delayMillis = 200)),
            ) {
                Text(
                    text = "Opening your first trip…",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
