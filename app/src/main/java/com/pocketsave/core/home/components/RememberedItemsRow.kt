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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.CardShadowColor
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.PSSectionHeader
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.core.home.pressScale

data class RememberedItem(
    val id: String,
    val name: String,
    val priceLabel: String,
    val unit: String?,
    val categoryIcon: ImageVector,
    val categoryTint: Color,
    val iconTint: Color,
    /** Stable icon-key → resolves to the same emoji the vault uses. */
    val iconKey: String? = null,
    /** Stored category colour hex for the emoji tile's soft tint. */
    val categoryColorHex: String? = null,
)

/**
 * "Your usuals" — the user's saved vault entries with a remembered shopping
 * price. Hides the whole section when no prices exist (no forced empty
 * state) so Home never feels like it's nagging for data.
 */
@Composable
fun RememberedItemsRow(
    items: List<RememberedItem>,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val pastels = PocketSaveTokens.pastels
    Column(modifier = modifier.fillMaxWidth()) {
        PSSectionHeader(
            title = "Your usuals",
            kicker = "prices we remember",
            accent = pastels.butterDeep,
            subtitle = "What you paid last — carried forward.",
            onSeeAll = onOpenVault,
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { item ->
                val index = items.indexOf(item)
                val shape = if (index % 2 == 0) AppShapes.Pebble else AppShapes.PebbleAlt
                RememberedItemCard(item = item, shape = shape, onClick = onOpenVault)
            }
        }
    }
}

@Composable
private fun RememberedItemCard(item: RememberedItem, shape: Shape, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .width(178.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .grainOverlay(tint = pastels.grain, density = 0.5f)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.pocketsave.core.vault.icons.CategoryEmojiTile(
                    iconKey = item.iconKey,
                    colorHex = item.categoryColorHex,
                    size = 36.dp,
                    cornerRadius = 11.dp,
                    fallbackTint = item.categoryTint,
                )
                Spacer(Modifier.weight(1f))
                SparkBadge()
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = item.priceLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = pastels.peachDeep,
                )
                if (!item.unit.isNullOrBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "/ ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SparkBadge() {
    // "We learned this" — a small butter-toned sticker so the remembered
    // price feels earned rather than arbitrary.
    val pastels = PocketSaveTokens.pastels
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(pastels.butterSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = pastels.butterDeep,
            modifier = Modifier.size(13.dp),
        )
    }
}
