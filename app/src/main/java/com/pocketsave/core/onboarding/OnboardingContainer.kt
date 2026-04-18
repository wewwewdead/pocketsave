package com.pocketsave.core.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Port of `OnboardingContainer` from `PocketSave/Core/Onboarding/OnboardingContainer.swift`.
 *
 * Hosts the step machine; when `onboardingComplete` flips the enclosing navigator
 * swaps this destination for Home, matching the iOS `ContentView` branch.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = viewModel.currentStep,
            label = "onboarding-step",
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val offset = if (forward) 1 else -1
                (
                    slideInHorizontally(tween(400)) { full -> offset * full } + fadeIn(tween(400))
                ) togetherWith (
                    slideOutHorizontally(tween(400)) { full -> -offset * full } + fadeOut(tween(400))
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> OnboardingWelcomeScreen(viewModel)
                OnboardingStep.LAST_STORE -> OnboardingLastStoreScreen(viewModel)
                OnboardingStep.FIRST_ITEM -> OnboardingFirstItemScreen(viewModel)
                OnboardingStep.DONE -> OnboardingDoneScreen()
            }
        }

        if (viewModel.showPageIndicator &&
            (viewModel.currentStep == OnboardingStep.LAST_STORE ||
                viewModel.currentStep == OnboardingStep.FIRST_ITEM)
        ) {
            Box(modifier = Modifier.padding(top = 12.dp)) {
                PageIndicator(currentStep = viewModel.currentStep)
            }
        }
    }
}
