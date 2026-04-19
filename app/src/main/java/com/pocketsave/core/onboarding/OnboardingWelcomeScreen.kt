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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.decor.UnderlineSwoosh
import com.pocketsave.common.ui.decor.blobDecor
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection

/**
 * Welcome — a warm, editorial first impression. Layered pastel blobs sit
 * behind a display-serif wordmark; an italic kicker above it carries a
 * hand-drawn swoosh. No progress bar, no secondary CTA — the opening frame
 * is a mood, not a tour.
 */
@Composable
fun OnboardingWelcomeScreen(viewModel: OnboardingViewModel) {
    val pastels = PocketSaveTokens.pastels
    OnboardingScaffold(
        progress = null,
        primaryCta = {
            Button(onClick = { viewModel.navigateToValue() }) {
                Text(
                    text = "Let's begin",
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingSection(delayMs = 60) {
                Box(
                    modifier = Modifier
                        .size(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Layered blob backdrop.
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .blobDecor(color = pastels.peachSoft, seed = 7),
                    )
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .blobDecor(color = pastels.butterSoft, seed = 21)
                            .padding(start = 40.dp, top = 20.dp),
                    )
                    // Grain wash to give the blob papery depth.
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .grainOverlay(tint = pastels.grain, density = 1.2f),
                    )
                    // Hero sage dot — brand spine.
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFlorist,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            OnboardingSection(delayMs = 200) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "a lovable grocery companion",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.9.sp,
                        ),
                        color = pastels.peachDeep,
                    )
                    Spacer(Modifier.width(6.dp))
                    UnderlineSwoosh(color = pastels.peachDeep.copy(alpha = 0.55f))
                }
            }

            Spacer(Modifier.height(10.dp))

            OnboardingSection(delayMs = 320) {
                Text(
                    text = "PocketSave",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = pastels.inkBerry,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(12.dp))

            OnboardingSection(delayMs = 440) {
                Text(
                    text = "Your grocery prices, remembered — so every trip feels a little softer.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
        }
    }
}
