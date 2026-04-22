package com.pocketsave.common.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.Motion
import com.pocketsave.common.ui.decor.softHairline
import com.pocketsave.common.ui.pressScale

/**
 * Sticker-flavoured chip. Softer than an M3 FilterChip, with an inner
 * icon bubble, a paper-cut hairline, and a springy press-scale. Selection
 * state animates the tint + ink with a short tween so the row reads as
 * one cohesive motion.
 */
@Composable
fun StickerChip(
    label: String,
    icon: ImageVector? = null,
    selected: Boolean,
    tint: Color,
    selectedTint: Color,
    ink: Color,
    selectedInk: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    /** When provided, renders this emoji glyph in the leading bubble instead
     *  of the Material icon. Lets the vault filter strip show the same
     *  category emoji as the manager sheet. */
    leadingEmoji: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) selectedTint else tint,
        animationSpec = tween(Motion.MediumMs),
        label = "sticker-bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) selectedInk else ink,
        animationSpec = tween(Motion.MediumMs),
        label = "sticker-ink",
    )
    Row(
        modifier = modifier
            .clip(AppShapes.Sticker)
            .background(bg)
            .softHairline(
                color = textColor.copy(alpha = if (selected) 0.18f else 0.08f),
                cornerRadius = 16.dp,
            )
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingEmoji != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    text = leadingEmoji,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                    ),
                )
            }
            Spacer(Modifier.size(7.dp))
        } else if (icon != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.size(7.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = textColor,
            maxLines = 1,
        )
        if (trailing != null) {
            Spacer(Modifier.size(6.dp))
            trailing()
        }
    }
}
