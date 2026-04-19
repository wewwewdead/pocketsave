package com.pocketsave.core.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.ArrowOutward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.util.ColorOption
import com.pocketsave.core.home.pressScale

data class VaultCategoryTile(
    val id: String,
    val name: String,
    val itemCount: Int,
    val icon: ImageVector,
    val tint: Color,
    val iconTint: Color,
)

/**
 * Horizontal row of compact category chips. Taps on any chip (or the section
 * header) jump to the full vault — same destination, different affordances —
 * because the vault screen is where categories live in this app.
 */
@Composable
fun VaultPreviewRow(
    categories: List<VaultCategoryTile>,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Your pantry",
            subtitle = if (categories.isEmpty())
                "Your categories live here"
            else
                "${categories.size} categories in your vault",
            onSeeAll = onOpenVault,
        )
        Spacer(Modifier.height(12.dp))
        if (categories.isEmpty()) {
            EmptyHint(message = "Add your first item in the vault.")
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                items(categories, key = { it.id }) { tile ->
                    CategoryChip(tile = tile, onClick = onOpenVault)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(tile: VaultCategoryTile, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .width(128.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(tile.tint)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = tile.iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = tile.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tile.iconTint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${tile.itemCount} item${if (tile.itemCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = tile.iconTint.copy(alpha = 0.72f),
            )
        }
    }
}

/**
 * Shared section header (title + subtitle + "See all" chip). Used by every
 * horizontal-list section so the rhythm stays consistent.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String?,
    onSeeAll: (() -> Unit)? = null,
    actionLabel: String = "See all",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onSeeAll != null) {
            val interaction = remember { MutableInteractionSource() }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .pressScale(interaction, pressedScale = 0.94f)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onSeeAll,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyHint(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
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
 * Cycles tints across category chips. Under the sage-only system every step
 * is a different intensity of the same family, so chips feel distinct
 * without breaking the one-accent rule. A persisted category `colorHex`
 * still wins for users who deliberately picked a swatch.
 */
object VaultPaletteCycle {
    fun tintFor(index: Int, storedHex: String?): Pair<Color, Color> {
        storedHex?.let { hex ->
            ColorOption.byHex(hex)?.let { return it.color to contrastInk(it.color) }
        }
        return SAGE_CYCLE[index.mod(SAGE_CYCLE.size)]
    }

    private fun contrastInk(bg: Color): Color {
        val luminance = (bg.red * 299 + bg.green * 587 + bg.blue * 114) / 1000f
        return if (luminance > 0.78f) Color(0xFF2D4E3D) else Color(0xFF1C2A20)
    }

    // Sage tonal ramp — lightest → deepest. Each step keeps the same hue and
    // only shifts intensity, which is what gives the vault row its calm,
    // single-family feel.
    private val SAGE_CYCLE: List<Pair<Color, Color>> = listOf(
        Color(0xFFEFF5F1) to Color(0xFF3F6351), // sage 50
        Color(0xFFE8F0EA) to Color(0xFF3F6351), // sage 75
        Color(0xFFE2EDE6) to Color(0xFF2D4E3D), // sage 100
        Color(0xFFD6E6DB) to Color(0xFF2D4E3D), // sage 200
        Color(0xFFC9DECF) to Color(0xFF22402F), // sage 300
        Color(0xFFE8F0EA) to Color(0xFF4C735F), // sage 75 (alt ink)
    )
}
