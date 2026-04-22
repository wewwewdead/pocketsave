package com.pocketsave.core.vault.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.core.vault.icons.AppIcon
import com.pocketsave.core.vault.icons.CategoryEmoji

/**
 * Grid the user picks a category icon from. Each tile renders the emoji
 * that category will wear across the app, so the preview matches the final
 * result one-to-one. Selected tile wears a ring in the currently-picked
 * colour.
 *
 * Keys written back are the stable strings in [AppIcon.registry]; resolving
 * them to emoji happens at render time so the data layer stores only the
 * key string.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun IconGridPicker(
    selectedKey: String?,
    tintColor: Color,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon.pickableIcons.forEach { (key, _) ->
            EmojiTile(
                key = key,
                emoji = CategoryEmoji.resolve(key),
                selected = key == selectedKey,
                tintColor = tintColor,
                onClick = { onSelect(key) },
            )
        }
    }
}

@Composable
private fun EmojiTile(
    key: String,
    emoji: String,
    selected: Boolean,
    tintColor: Color,
    onClick: () -> Unit,
) {
    val outline = if (selected) tintColor else Color(0x22000000)
    val borderWidth = if (selected) 2.dp else 1.dp
    val bg = if (selected) tintColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(borderWidth, outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = TextStyle(fontSize = 26.sp),
        )
    }
}
