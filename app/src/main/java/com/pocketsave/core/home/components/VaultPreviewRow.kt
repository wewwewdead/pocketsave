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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.PastelPalette
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.components.AffectionateEmpty
import com.pocketsave.common.ui.components.PSSectionHeader
import com.pocketsave.common.ui.decor.grainOverlay
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.home.pressScale

data class VaultCategoryTile(
    val id: String,
    val name: String,
    val itemCount: Int,
    val icon: ImageVector,
    val tint: Color,
    val iconTint: Color,
    /** Stable icon-key (maps to [com.pocketsave.core.vault.icons.CategoryEmoji]).
     *  When present the home chip renders the emoji sticker — matching the
     *  Vault screen — instead of the Material icon. */
    val iconKey: String? = null,
    /** Raw hex for the category's stored colour, passed through so the
     *  emoji tile can carry the same soft tint used in the vault. */
    val colorHex: String? = null,
)

/**
 * Horizontal row of category chips. The whole section (header, chips,
 * empty state) funnels taps to [onOpenVault] since that's where categories
 * actually live.
 */
@Composable
fun VaultPreviewRow(
    categories: List<VaultCategoryTile>,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    Column(modifier = modifier.fillMaxWidth()) {
        PSSectionHeader(
            title = "Your vault",
            kicker = "the collection",
            accent = pastels.lavenderDeep,
            subtitle = if (categories.isEmpty())
                "Where your usuals come to live."
            else
                "${categories.size} categor${if (categories.size == 1) "y" else "ies"} saved",
            onSeeAll = onOpenVault,
        )
        Spacer(Modifier.height(12.dp))
        if (categories.isEmpty()) {
            AffectionateEmpty(
                title = "A little empty in here.",
                body = "Add the things you buy often — we'll remember prices and carry them into each trip.",
                icon = Icons.Outlined.Inventory2,
                accent = pastels.lavenderDeep,
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(categories, key = { it.id }) { tile ->
                    val index = categories.indexOf(tile)
                    val shape = if (index % 2 == 0) AppShapes.Pebble else AppShapes.PebbleAlt
                    CategoryChip(tile = tile, shape = shape, onClick = onOpenVault)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(tile: VaultCategoryTile, shape: Shape, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .width(132.dp)
            .clip(shape)
            .background(tile.tint)
            .grainOverlay(tint = pastels.grain, density = 0.6f)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            com.pocketsave.core.vault.icons.CategoryEmojiTile(
                iconKey = tile.iconKey,
                colorHex = tile.colorHex,
                size = 40.dp,
                cornerRadius = 12.dp,
                fallbackTint = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = tile.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tile.iconTint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${tile.itemCount} saved",
                style = MaterialTheme.typography.labelSmall,
                color = tile.iconTint.copy(alpha = 0.72f),
            )
        }
    }
}

/**
 * Legacy section header (kept for callers that still import this symbol).
 * New code should use `PSSectionHeader` from `common.ui.components`.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String?,
    onSeeAll: (() -> Unit)? = null,
    actionLabel: String = "See all",
) {
    PSSectionHeader(
        title = title,
        subtitle = subtitle,
        onSeeAll = onSeeAll,
        actionLabel = actionLabel,
    )
}

/**
 * Small text-only empty placeholder. Preserved for legacy callers; prefer
 * [AffectionateEmpty] for new surfaces.
 */
@Composable
internal fun EmptyHint(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
        )
    }
}

/**
 * Editorial tint cycle for category chips. Walks the supporting-hue cast —
 * mint, peach, butter, lavender, sky — so the vault row reads as a small
 * cast of characters instead of one tonal ramp. A persisted [storedHex]
 * still wins for users who deliberately picked a swatch.
 */
object VaultPaletteCycle {
    /**
     * Resolves a tint pair from the supporting-hue cast. Non-composable by
     * design so callers can use it inside `remember { ... mapIndexed { ... }}`.
     * The caller resolves [palette] once at composition scope and passes it
     * in for every item.
     */
    fun tintFor(
        palette: PastelPalette,
        index: Int,
        storedHex: String?,
    ): Pair<Color, Color> {
        storedHex?.let { hex ->
            ColorOption.byHex(hex)?.let { return it.color to contrastInk(it.color) }
        }
        val cycle = listOf(
            palette.mintSoft to palette.mintDeep,
            palette.peachSoft to palette.peachDeep,
            palette.butterSoft to palette.butterDeep,
            palette.lavenderSoft to palette.lavenderDeep,
            palette.skySoft to palette.skyDeep,
        )
        return cycle[index.mod(cycle.size)]
    }

    private fun contrastInk(bg: Color): Color {
        val luminance = (bg.red * 299 + bg.green * 587 + bg.blue * 114) / 1000f
        return if (luminance > 0.78f) Color(0xFF2D4E3D) else Color(0xFF1C2A20)
    }
}
