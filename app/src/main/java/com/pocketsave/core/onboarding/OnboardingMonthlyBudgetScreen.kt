package com.pocketsave.core.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.onboarding.components.PriceField
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection
import kotlinx.coroutines.delay

/**
 * Optional monthly-budget step that sits right after Currency. Writing a
 * value here lets the home pill and history card show a live spent/remaining
 * number from day one; leaving it blank writes `0.0`, which the rest of the
 * app treats as "budget not set" and hides the ratio UI until the user sets
 * one from More later.
 */
@Composable
fun OnboardingMonthlyBudgetScreen(viewModel: OnboardingViewModel) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val formatter = LocalCurrencyFormatter.current

    // Let the step transition + staggered reveal land before requesting focus
    // — same pattern as the Trip step; attaching to a not-yet-composed target
    // throws. 600ms matches the reveal delay on the input section.
    LaunchedEffect(Unit) {
        delay(600)
        runCatching { focusRequester.requestFocus() }
    }

    val parsed = viewModel.monthlyBudgetInput.trim().toDoubleOrNull()

    OnboardingScaffold(
        progress = viewModel.progressForStep,
        onBack = { viewModel.navigateBack() },
        primaryCta = {
            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.commitMonthlyBudgetAndContinue()
                },
            ) {
                Text("Continue", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingSection(delayMs = 40) {
                Text(
                    text = "Your monthly budget.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            Spacer(Modifier.height(6.dp))

            OnboardingSection(delayMs = 140) {
                Text(
                    text = "A soft ceiling for the month. The home pill climbs as you shop — no alerts, no shame.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    ),
                )
            }

            Spacer(Modifier.height(22.dp))

            OnboardingSection(delayMs = 260) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PriceField(
                        value = viewModel.monthlyBudgetInput,
                        onChange = { viewModel.monthlyBudgetInput = it },
                        label = "Monthly budget (optional)",
                        placeholder = "0.00",
                        imeAction = ImeAction.Done,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )

                    if (parsed != null && parsed > 0) {
                        Text(
                            text = "That's ${formatter.format(parsed)} for the month.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            OnboardingSection(delayMs = 360) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            viewModel.monthlyBudgetInput = ""
                            keyboard?.hide()
                            viewModel.commitMonthlyBudgetAndContinue()
                        },
                    ) {
                        Text("Skip for now")
                    }
                }
            }
        }
    }
}
