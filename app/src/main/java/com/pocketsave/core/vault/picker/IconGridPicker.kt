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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pocketsave.core.vault.icons.AppIcon

/**
 * Grid of Material Icons the user can choose from when creating / editing a
 * category. The selected tile wears a ring in the currently-selected colour
 * so the user can preview the combined (icon + colour) treatment before
 * saving.
 *
 * Keys written back are the stable strings in [AppIcon.registry]; resolving
 * them happens at render time everywhere else so the data layer never holds
 * an [ImageVector] reference.
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
        AppIcon.pickableIcons.forEach { (key, vector) ->
            IconTile(
                key = key,
                vector = vector,
                selected = key == selectedKey,
                tintColor = tintColor,
                onClick = { onSelect(key) },
            )
        }
    }
}

@Composable
private fun IconTile(
    key: String,
    vector: ImageVector,
    selected: Boolean,
    tintColor: Color,
    onClick: () -> Unit,
) {
    val outline = if (selected) tintColor else Color(0x22000000)
    val borderWidth = if (selected) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(borderWidth, outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vector,
            contentDescription = key,
            tint = if (selected) tintColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}
