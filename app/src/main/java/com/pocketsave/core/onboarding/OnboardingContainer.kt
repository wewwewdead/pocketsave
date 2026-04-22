package com.pocketsave.core.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.pocketsave.core.onboarding.motion.onboardingStepTransition

/**
 * Step host for the onboarding flow. Owns one [AnimatedContent] so every step
 * transition runs through the shared motion factory; each screen is
 * responsible for its own body + header/footer via [OnboardingScaffold].
 *
 * Back behaviour: the system back gesture walks the step machine back one
 * place rather than leaving the onboarding destination. On the first step
 * back is a no-op (caller is the nav host root).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingContainer(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
) {
    LaunchedEffect(viewModel.onboardingComplete) {
        if (viewModel.onboardingComplete) onComplete()
    }

    BackHandler(enabled = viewModel.currentStep != OnboardingStep.WELCOME) {
        viewModel.navigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = viewModel.currentStep,
            label = "onboarding-step",
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                onboardingStepTransition(forward = forward)
            },
            modifier = Modifier.fillMaxSize(),
        ) { step ->
            // iOS has exactly 4 onboarding steps: Welcome → LastStore → FirstItem → Done.
            // The VALUE / CURRENCY / MONTHLY_BUDGET / TRIP steps were Android-only
            // insertions; they're bypassed here so the flow matches iOS. Currency
            // and monthly budget are set later from the side menu instead.
            when (step) {
                OnboardingStep.WELCOME -> OnboardingWelcomeScreen(viewModel)
                OnboardingStep.STORE -> OnboardingLastStoreScreen(viewModel)
                OnboardingStep.ITEM -> OnboardingFirstItemScreen(viewModel)
                OnboardingStep.HANDOFF -> OnboardingHandoffScreen(viewModel)
                // These four steps are intentionally no-ops — the container routes
                // around them. Left as an empty branch so the enum remains valid.
                OnboardingStep.VALUE,
                OnboardingStep.CURRENCY,
                OnboardingStep.MONTHLY_BUDGET,
                OnboardingStep.TRIP -> Unit
            }
        }
    }
}
