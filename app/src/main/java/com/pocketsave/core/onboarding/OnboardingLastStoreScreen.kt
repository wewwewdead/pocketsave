package com.pocketsave.core.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pocketsave.core.onboarding.motion.OnboardingScaffold
import com.pocketsave.core.onboarding.motion.OnboardingSection
import kotlinx.coroutines.delay

/**
 * "Where do you usually shop?" — the user's first store. Uses the shared
 * scaffold so the Continue CTA stays fixed above the keyboard; the field
 * auto-focuses on entry so the user can start typing immediately.
 *
 * Skip is a trailing header action — it keeps the step optional but doesn't
 * compete visually with Continue. If the user has already picked a currency
 * but doesn't want to name a store, the rest of the flow still works.
 */
@Composable
fun OnboardingLastStoreScreen(viewModel: OnboardingViewModel) {
    val form = viewModel.formViewModel
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Wait until the step transition has settled AND the text field's
    // staggered-reveal section has actually composed its content. Calling
    // requestFocus() before the field's modifier chain is attached throws
    // IllegalStateException: FocusRequester is not initialized.
    LaunchedEffect(Unit) {
        delay(520)
        runCatching { focusRequester.requestFocus() }
    }

    val advance: () -> Unit = {
        if (!form.isValidStoreName) {
            viewModel.triggerStoreNameError()
        } else {
            keyboard?.hide()
            viewModel.navigateToItem()
        }
    }

    OnboardingScaffold(
        progress = viewModel.progressForStep,
        onBack = { viewModel.navigateBack() },
        trailingHeader = {
            TextButton(
                onClick = {
                    viewModel.resetForSkip()
                    keyboard?.hide()
                    viewModel.navigateToItem()
                },
            ) { Text("Skip") }
        },
        primaryCta = {
            Button(
                onClick = advance,
                enabled = form.isValidStoreName,
            ) {
                Text("Continue", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(24.dp))

            OnboardingSection(delayMs = 40) {
                Text(
                    text = "Where do you\nusually shop?",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            Spacer(Modifier.height(6.dp))

            OnboardingSection(delayMs = 160) {
                Text(
                    text = "We'll remember prices by store — so next time you won't have to guess.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    ),
                )
            }

            Spacer(Modifier.height(28.dp))

            OnboardingSection(delayMs = 260) {
                OutlinedTextField(
                    value = form.storeName,
                    onValueChange = { new ->
                        form.storeName = new
                        if (form.isValidStoreName) viewModel.showError = false
                    },
                    placeholder = { Text("e.g. Trader Joe's") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { advance() }),
                    isError = viewModel.showError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }

            AnimatedVisibility(
                visible = viewModel.showError,
                enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { -it / 2 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 2 },
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "A store name needs at least one character.",
                        color = Color(0xFFFF6F71),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
