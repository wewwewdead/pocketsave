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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.CardShadowColor
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.home.pressScale

data class RememberedItem(
    val id: String,
    val name: String,
    val priceLabel: String,
    val unit: String?,
    val categoryIcon: ImageVector,
    val categoryTint: Color,
    val iconTint: Color,
)

/**
 * "We remember these prices" — shows the user's item vault entries with a
 * known shopping price so the app feels like it has context on their grocery
 * life. Scrolls horizontally and hides itself entirely when there's nothing to
 * say, so the page doesn't display an empty promise.
 */
@Composable
fun RememberedItemsRow(
    items: List<RememberedItem>,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "PocketSave remembers",
            subtitle = "Usual prices from your vault",
            onSeeAll = onOpenVault,
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                RememberedItemCard(item = item, onClick = onOpenVault)
            }
        }
    }
}

@Composable
private fun RememberedItemCard(item: RememberedItem, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .width(170.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(item.categoryTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.categoryIcon,
                        contentDescription = null,
                        tint = item.iconTint,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                SparkBadge()
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = item.priceLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (!item.unit.isNullOrBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "/ ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SparkBadge() {
    // A tiny "we learned this" mark so the price feels earned, not arbitrary.
    val pastels = PocketSaveTokens.pastels
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(pastels.butterSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Bolt,
            contentDescription = null,
            tint = pastels.butterDeep,
            modifier = Modifier.size(12.dp),
        )
    }
}
