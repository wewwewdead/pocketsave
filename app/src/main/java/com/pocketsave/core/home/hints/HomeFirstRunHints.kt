package com.pocketsave.core.home.hints

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Lightweight first-run coach marks shown once on Home immediately after
 * onboarding completes. Three sequential tips that anchor roughly to the
 * first cart card, the vault affordance, and the budget. Each tap advances;
 * the last tap calls [onDismissed] so Home can clear the preference flag.
 *
 * Intentionally non-blocking: the whole overlay has a subtle scrim users can
 * tap past, not a modal wall. Motion is fade + short slide so the bubble
 * arrives and leaves without stealing attention.
 */
@Composable
fun HomeFirstRunHints(
    visible: Boolean,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    var index by remember { mutableStateOf(0) }
    val hints = remember {
        listOf(
            Hint(
                icon = Icons.Default.ShoppingBasket,
                title = "Your first trip is ready.",
                body = "Tap a cart card to open it. That's where you'll add items as you shop.",
            ),
            Hint(
                icon = Icons.Default.Storefront,
                title = "Your vault lives here.",
                body = "Open the vault to build your list or bring items into any trip.",
            ),
            Hint(
                icon = Icons.Default.AttachMoney,
                title = "Budget tracks itself.",
                body = "As you mark items bought, the cart card shows how close you are.",
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x33000000))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = {
                    if (index >= hints.lastIndex) onDismissed() else index += 1
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            hints.forEachIndexed { i, hint ->
                AnimatedVisibility(
                    visible = i == index,
                    enter = fadeIn(tween(260)) +
                        slideInVertically(tween(280)) { full -> -(full * 0.12f).toInt() },
                    exit = fadeOut(tween(180)) +
                        slideOutVertically(tween(220)) { full -> -(full * 0.12f).toInt() },
                ) {
                    HintCard(
                        hint = hint,
                        position = i + 1,
                        total = hints.size,
                        isLast = i == hints.lastIndex,
                        onNext = {
                            if (i >= hints.lastIndex) onDismissed() else index = i + 1
                        },
                        onSkip = onDismissed,
                    )
                }
            }
        }
    }
}

private data class Hint(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

@Composable
private fun HintCard(
    hint: Hint,
    position: Int,
    total: Int,
    isLast: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 16.dp,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = hint.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hint.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Text(
                        text = "Tip $position of $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = hint.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onSkip) { Text(if (isLast) "Close" else "Skip") }
                Button(onClick = onNext) {
                    Text(if (isLast) "Got it" else "Next")
                }
            }
        }
    }
}
