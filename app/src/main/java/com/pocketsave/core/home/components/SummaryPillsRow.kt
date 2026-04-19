package com.pocketsave.core.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.home.pressScale

/**
 * Three soft-pastel summary pills: active trips, budget remaining, saved items.
 * Each pill is visually balanced — the numeric value leads, the label follows —
 * so the eye can read the row at a glance without scanning for hierarchy.
 *
 * [onClick] is optional per pill; pills without a handler render as plain cards
 * (no press feedback). Pills with a handler pick up the shared spring
 * press-scale so the tap feels consistent with the rest of the page.
 */
data class SummaryPill(
    val label: String,
    val value: String,
    val tint: Color,
    val iconTint: Color,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun SummaryPillsRow(
    activeTrips: Int,
    budgetRemainingLabel: String,
    savedItems: Int,
    onOpenVault: () -> Unit,
    onOpenActiveTrips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val pills = listOf(
        SummaryPill(
            label = "Active trips",
            value = activeTrips.toString(),
            tint = pastels.mintSoft,
            iconTint = pastels.mintDeep,
            icon = Icons.Outlined.LocalMall,
            onClick = onOpenActiveTrips,
        ),
        SummaryPill(
            label = "Budget left",
            value = budgetRemainingLabel,
            tint = pastels.peachSoft,
            iconTint = pastels.peachDeep,
            icon = Icons.Outlined.Savings,
        ),
        SummaryPill(
            label = "Saved items",
            value = savedItems.toString(),
            tint = pastels.lavenderSoft,
            iconTint = pastels.lavenderDeep,
            icon = Icons.Outlined.Bookmark,
            onClick = onOpenVault,
        ),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        pills.forEach { pill ->
            SummaryPillCard(pill = pill, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryPillCard(
    pill: SummaryPill,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val tapModifier = pill.onClick?.let { handler ->
        Modifier
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = handler,
            )
    } ?: Modifier

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pill.tint)
            .then(tapModifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = pill.icon,
                    contentDescription = null,
                    tint = pill.iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = pill.value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = pill.iconTint,
            maxLines = 1,
        )
        Text(
            text = pill.label,
            style = MaterialTheme.typography.labelSmall,
            color = pill.iconTint.copy(alpha = 0.78f),
            maxLines = 1,
        )
    }
}
