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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.decor.UnderlineSwoosh
import com.pocketsave.common.ui.decor.blobDecor
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection

/**
 * Three quiet value lines. Each icon sits inside a tinted blob — different
 * supporting hue per line — so the screen reads as a small editorial set
 * instead of a bullet list.
 */
@Composable
fun OnboardingValueScreen(viewModel: OnboardingViewModel) {
    val pastels = PocketSaveTokens.pastels
    OnboardingScaffold(
        progress = null,
        onBack = { viewModel.navigateBack() },
        primaryCta = {
            Button(onClick = { viewModel.navigateToCurrency() }) {
                Text(
                    text = "Continue",
                    modifier = Modifier.padding(horizontal = 10.dp),
                    fontWeight = FontWeight.SemiBold,
                )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "what it does",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.9.sp,
                        ),
                        color = pastels.mintDeep,
                    )
                    Spacer(Modifier.width(6.dp))
                    UnderlineSwoosh(color = pastels.mintDeep.copy(alpha = 0.55f))
                }
            }

            Spacer(Modifier.height(8.dp))

            OnboardingSection(delayMs = 160) {
                Text(
                    text = "A calmer way\nto shop.",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = pastels.inkBerry,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.widthIn(max = 360.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            OnboardingSection(delayMs = 260) {
                Text(
                    text = "Three quiet things PocketSave does — so you don't have to think about them.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier.widthIn(max = 360.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            OnboardingSection(delayMs = 360) {
                ValueRow(
                    icon = Icons.Rounded.Savings,
                    title = "Remembers your prices",
                    body = "What you paid last time, at each store.",
                    tint = pastels.peachSoft,
                    deepTint = pastels.peachDeep,
                    blobSeed = 3,
                )
            }
            Spacer(Modifier.height(14.dp))
            OnboardingSection(delayMs = 460) {
                ValueRow(
                    icon = Icons.Rounded.ShoppingBasket,
                    title = "Turns lists into trips",
                    body = "Pick items, set a budget, shop with confidence.",
                    tint = pastels.butterSoft,
                    deepTint = pastels.butterDeep,
                    blobSeed = 9,
                )
            }
            Spacer(Modifier.height(14.dp))
            OnboardingSection(delayMs = 560) {
                ValueRow(
                    icon = Icons.Rounded.CreditCard,
                    title = "Stays on your phone",
                    body = "Offline-first. No accounts, no sync to set up.",
                    tint = pastels.lavenderSoft,
                    deepTint = pastels.lavenderDeep,
                    blobSeed = 15,
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
    tint: Color,
    deepTint: Color,
    blobSeed: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .blobDecor(color = tint, seed = blobSeed),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = deepTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.padding(top = 4.dp)) {
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                ),
            )
        }
    }
}
