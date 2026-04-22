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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.core.home.pressScale

data class QuickAction(
    val label: String,
    val caption: String,
    val icon: ImageVector,
    val tint: Color,
    val iconTint: Color,
    val shape: Shape,
    val onClick: () -> Unit,
    val isPrimary: Boolean = false,
)

/**
 * Four-up grid of soft action tiles. The New-trip tile is the sage CTA; the
 * other three carry the supporting-hue cast (peach for vault, butter for
 * history, lavender for trash) so each shortcut has its own identity note
 * without the row turning into a rainbow.
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
    val actions = listOf(
        QuickAction(
            label = "New trip",
            caption = "Set a budget, go shop",
            icon = Icons.Outlined.AddShoppingCart,
            tint = MaterialTheme.colorScheme.primary,
            iconTint = MaterialTheme.colorScheme.onPrimary,
            shape = AppShapes.Pebble,
            onClick = onNewTrip,
            isPrimary = true,
        ),
        QuickAction(
            label = "Your vault",
            caption = "Saved items",
            icon = Icons.Outlined.Inventory2,
            tint = pastels.peachSoft,
            iconTint = pastels.peachDeep,
            shape = AppShapes.PebbleAlt,
            onClick = onOpenVault,
        ),
        QuickAction(
            label = "Past trips",
            caption = "Look back",
            icon = Icons.Outlined.HistoryToggleOff,
            tint = pastels.butterSoft,
            iconTint = pastels.butterDeep,
            shape = AppShapes.PebbleAlt,
            onClick = onOpenHistory,
        ),
        QuickAction(
            label = "Little bin",
            caption = "Restore carts",
            icon = Icons.Outlined.DeleteOutline,
            tint = pastels.lavenderSoft,
            iconTint = pastels.lavenderDeep,
            shape = AppShapes.Pebble,
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
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    val bubbleBg = if (action.isPrimary) {
        Color.White.copy(alpha = 0.2f)
    } else {
        Color.White.copy(alpha = 0.65f)
    }
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .clip(action.shape)
            .background(action.tint)
            .grainOverlay(
                tint = if (action.isPrimary) Color.White.copy(alpha = 0.06f) else pastels.grain,
                density = 0.6f,
            )
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
                    .size(42.dp)
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
