package com.pocketsave.core.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection

/**
 * Welcome — a calm, premium first screen. No progress bar, no secondary CTA;
 * we want the first frame to feel like an inviting opening shot, not a tour.
 */
@Composable
fun OnboardingWelcomeScreen(viewModel: OnboardingViewModel) {
    OnboardingScaffold(
        progress = null,
        primaryCta = {
            Button(onClick = { viewModel.navigateToValue() }) {
                Text("Get started", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingSection(delayMs = 60) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            Spacer(Modifier.height(32.dp))

            OnboardingSection(delayMs = 180) {
                Text(
                    text = "Your grocery prices,\nremembered.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 340.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            OnboardingSection(delayMs = 320) {
                Text(
                    text = "PocketSave keeps track of what you paid, where — so you can plan trips and stay on budget without second-guessing.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
        }
    }
}
