package com.pocketsave.core.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection

/**
 * Short value screen. Three quiet lines, no carousel, no long copy. We stagger
 * the entry so each value point lands a beat apart — enough for the eye to
 * catch each one without feeling like a bullet list.
 */
@Composable
fun OnboardingValueScreen(viewModel: OnboardingViewModel) {
    OnboardingScaffold(
        progress = null,
        onBack = { viewModel.navigateBack() },
        primaryCta = {
            Button(onClick = { viewModel.navigateToCurrency() }) {
                Text("Continue", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            OnboardingSection(delayMs = 60) {
                Text(
                    text = "A calmer way\nto shop.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.widthIn(max = 360.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            OnboardingSection(delayMs = 180) {
                Text(
                    text = "Three things PocketSave does quietly, so you don't have to think about them.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    ),
                    modifier = Modifier.widthIn(max = 360.dp),
                )
            }

            Spacer(Modifier.height(32.dp))

            OnboardingSection(delayMs = 280) {
                ValueRow(
                    icon = Icons.Rounded.Savings,
                    title = "Remembers your prices",
                    body = "What you paid last time, at each store.",
                )
            }
            Spacer(Modifier.height(16.dp))
            OnboardingSection(delayMs = 380) {
                ValueRow(
                    icon = Icons.Rounded.ShoppingBasket,
                    title = "Turns lists into trips",
                    body = "Pick items, set a budget, shop with confidence.",
                )
            }
            Spacer(Modifier.height(16.dp))
            OnboardingSection(delayMs = 480) {
                ValueRow(
                    icon = Icons.Rounded.CreditCard,
                    title = "Stays on your phone",
                    body = "Offline-first. No accounts, no sync to set up.",
                )
            }
        }
    }
}

@Composable
private fun ValueRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                ),
            )
        }
    }
}

