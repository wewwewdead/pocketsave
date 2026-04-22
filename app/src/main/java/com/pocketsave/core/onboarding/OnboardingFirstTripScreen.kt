package com.pocketsave.core.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.onboarding.components.PriceField
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection
import com.pocketsave.core.onboarding.motion.onboardingCelebrationPulse
import kotlinx.coroutines.delay

/**
 * The "aha" step — user names their first trip and sets a soft budget. We
 * show the just-saved item as a small confirmation card so the user can feel
 * that the previous step landed, then invite them to "Create trip." Persists
 * via [OnboardingViewModel.createTripAndFinish].
 */
@Composable
fun OnboardingFirstTripScreen(viewModel: OnboardingViewModel) {
    val form = viewModel.formViewModel
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val formatter = LocalCurrencyFormatter.current
    val haptics = rememberAppHaptics()

    // Haptics for the trip step. The Confirm fires once per successful
    // create; the Reject fires once per distinct validation message the
    // ViewModel publishes (e.g., "Save your first item before creating a
    // trip.", "Give your trip a name.").
    LaunchedEffect(viewModel.tripCelebrationTrigger) {
        if (viewModel.tripCelebrationTrigger > 0) haptics.perform(AppHaptic.Confirm)
    }
    LaunchedEffect(viewModel.tripError) {
        if (viewModel.tripError != null) haptics.perform(AppHaptic.Reject)
    }

    // Wait for the step transition AND the trip-name section's staggered
    // reveal to compose the field before requesting focus — otherwise the
    // FocusRequester has no attached target and requestFocus() throws.
    LaunchedEffect(Unit) {
        delay(600)
        runCatching { focusRequester.requestFocus() }
    }

    val budgetValue = viewModel.tripBudget.toDoubleOrNull()
    val canCreate = viewModel.tripName.trim().isNotEmpty()

    OnboardingScaffold(
        progress = viewModel.progressForStep,
        onBack = { viewModel.navigateBack() },
        primaryCta = {
            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.createTripAndFinish()
                },
                enabled = canCreate,
                modifier = Modifier.onboardingCelebrationPulse(viewModel.tripCelebrationTrigger),
            ) {
                Text("Create trip", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingSection(delayMs = 40) {
                Text(
                    text = "Your first trip.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            Spacer(Modifier.height(6.dp))

            OnboardingSection(delayMs = 140) {
                Text(
                    text = "Give it a name and a soft budget. We'll track spending as you shop.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    ),
                )
            }

            Spacer(Modifier.height(18.dp))

            OnboardingSection(delayMs = 220) {
                FirstItemBadge(
                    itemName = form.itemName.ifBlank { "Your first item" },
                    storeName = form.storeName.ifBlank { null },
                )
            }

            Spacer(Modifier.height(20.dp))

            OnboardingSection(delayMs = 320) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = viewModel.tripName,
                        onValueChange = { viewModel.tripName = it },
                        label = { Text("Trip name") },
                        placeholder = { Text("e.g. Weekend shop") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )

                    PriceField(
                        value = viewModel.tripBudget,
                        onChange = { viewModel.tripBudget = it },
                        label = "Budget (optional)",
                        placeholder = "0.00",
                        imeAction = ImeAction.Done,
                    )

                    if (budgetValue != null && budgetValue > 0) {
                        Text(
                            text = "Aiming for about ${formatter.format(budgetValue)}.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            ),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = viewModel.tripError != null,
                enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { -it / 2 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 2 },
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = viewModel.tripError.orEmpty(),
                        color = Color(0xFFFF6F71),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstItemBadge(itemName: String, storeName: String?) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ready to add",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
                Text(
                    text = buildString {
                        append(itemName)
                        if (!storeName.isNullOrBlank()) append(" · $storeName")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
