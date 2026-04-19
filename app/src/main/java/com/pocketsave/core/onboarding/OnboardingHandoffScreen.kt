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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.CelebrationBurst
import com.pocketsave.common.ui.decor.blobDecor
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.core.onboarding.motion.onboardingCelebrationPulse
import kotlinx.coroutines.delay

/**
 * Brief handoff moment shown between the Trip step and Home. Adds a
 * celebratory confetti burst + layered blobs so finishing onboarding feels
 * like the small event it is. The container pops this screen on
 * `onComplete()` firing.
 */
@Composable
fun OnboardingHandoffScreen(viewModel: OnboardingViewModel) {
    val pastels = PocketSaveTokens.pastels
    var appear by remember { mutableStateOf(false) }
    var burstTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(40)
        appear = true
        delay(120)
        burstTrigger = 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Celebration layer — fills the screen so the burst emanates from
        // the centre without clipping. Stays above the background but below
        // the check-mark so the glyph reads clearly.
        CelebrationBurst(trigger = burstTrigger)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = appear,
                enter = fadeIn(tween(320)),
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .blobDecor(color = pastels.peachSoft, seed = 5),
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .grainOverlay(tint = pastels.grain, density = 1.2f),
                    )
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .onboardingCelebrationPulse(viewModel.tripCelebrationTrigger),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = appear,
                enter = fadeIn(tween(360, delayMillis = 80)),
            ) {
                Text(
                    text = "All set.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = pastels.inkBerry,
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
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }
        }
    }
}
