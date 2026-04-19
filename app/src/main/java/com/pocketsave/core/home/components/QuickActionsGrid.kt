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
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

data class QuickAction(
    val label: String,
    val caption: String,
    val icon: ImageVector,
    val tint: Color,
    val iconTint: Color,
    val onClick: () -> Unit,
    val isPrimary: Boolean = false,
)

/**
 * Two-row grid of four soft tiles. Uses Row-of-Rows instead of LazyVerticalGrid
 * because the count is fixed and we want predictable measurement inside the
 * parent LazyColumn.
 */
@Composable
fun QuickActionsGrid(
    onNewTrip: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    // New trip is the primary CTA (sage filled); the other three sit in
    // softer sage tonals so the row reads as one family.
    val actions = listOf(
        QuickAction(
            label = "New trip",
            caption = "Start shopping",
            icon = Icons.Outlined.AddShoppingCart,
            tint = MaterialTheme.colorScheme.primary,
            iconTint = MaterialTheme.colorScheme.onPrimary,
            onClick = onNewTrip,
            isPrimary = true,
        ),
        QuickAction(
            label = "Vault",
            caption = "Your pantry",
            icon = Icons.Outlined.Inventory2,
            tint = pastels.peachSoft,
            iconTint = pastels.peachDeep,
            onClick = onOpenVault,
        ),
        QuickAction(
            label = "History",
            caption = "Past trips",
            icon = Icons.Outlined.HistoryToggleOff,
            tint = pastels.mintSoft,
            iconTint = pastels.mintDeep,
            onClick = onOpenHistory,
        ),
        QuickAction(
            label = "Trash",
            caption = "Restore carts",
            icon = Icons.Outlined.DeleteOutline,
            tint = pastels.lavenderSoft,
            iconTint = pastels.lavenderDeep,
            onClick = onOpenTrash,
        ),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(actions[0], modifier = Modifier.weight(1f))
            ActionTile(actions[1], modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(actions[2], modifier = Modifier.weight(1f))
            ActionTile(actions[3], modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionTile(
    action: QuickAction,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    // Primary tile pairs a white bubble against the sage fill; soft tiles
    // use a translucent-white bubble against a sage tint so every tile
    // shares the same tonal contrast relationship.
    val bubbleBg = if (action.isPrimary) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.6f)
    }
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(action.tint)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = action.onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bubbleBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = action.iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = action.iconTint,
                    maxLines = 1,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = action.caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = action.iconTint.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
        }
    }
}
