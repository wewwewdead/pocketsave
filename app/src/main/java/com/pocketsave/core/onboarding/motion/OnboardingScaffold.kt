package com.pocketsave.core.onboarding.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared chrome for every onboarding screen. Keeps the header row (back +
 * progress) and footer CTA region fixed so the keyboard can appear/disappear
 * without the CTA leaping, and so the progress bar animates smoothly across
 * transitions instead of re-mounting on each screen.
 *
 * Content grows and shrinks via [animateContentSize] so inline errors,
 * pickers, and helper text settle instead of popping.
 *
 * @param progress 0f..1f to show the progress bar; null hides it.
 * @param onBack back handler — rendered only when non-null.
 * @param trailingHeader optional slot to the right of the progress bar (e.g.
 *   a "Skip" text button).
 * @param primaryCta footer-right slot. Kept fixed above the IME.
 * @param secondaryCta footer-left slot for secondary action.
 * @param footerCenter optional footer centered slot (e.g. helper copy).
 */
@Composable
fun OnboardingScaffold(
    progress: Float?,
    onBack: (() -> Unit)? = null,
    trailingHeader: (@Composable () -> Unit)? = null,
    primaryCta: (@Composable () -> Unit)? = null,
    secondaryCta: (@Composable () -> Unit)? = null,
    footerCenter: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Header row: back button (or 48dp spacer) + progress + trailing slot.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    )
                }
            } else {
                Box(modifier = Modifier.size(48.dp))
            }

            AnimatedVisibility(
                visible = progress != null,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180)),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                OnboardingProgressBar(progress = progress ?: 0f)
            }

            if (trailingHeader != null) {
                trailingHeader()
            } else {
                Box(modifier = Modifier.width(48.dp))
            }
        }

        // Body — fills available space. Size settling is handled per-screen via
        // AnimatedVisibility on the pieces that actually need it (errors,
        // conditional rows); applying animateContentSize here would force an
        // intrinsic measure pass that crashes LazyColumn / verticalScroll
        // children (Currency + Item steps).
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            content = content,
        )

        // Footer CTA region. Drawn above the IME because of imePadding() above.
        if (primaryCta != null || secondaryCta != null || footerCenter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    if (secondaryCta != null) secondaryCta() else Box(modifier = Modifier.width(0.dp))
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (footerCenter != null) footerCenter()
                }
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (primaryCta != null) primaryCta() else Box(modifier = Modifier.width(0.dp))
                }
            }
        }
    }
}

/**
 * Default backdrop color for onboarding. Slightly warmer than pure surface so
 * Welcome feels inviting; individual screens can override if needed.
 */
val OnboardingBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background
