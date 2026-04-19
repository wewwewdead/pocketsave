package com.pocketsave.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.decor.UnderlineSwoosh
import com.pocketsave.common.ui.pressScale

/**
 * The app's single section header. Display-serif title, optional italic kicker
 * above it, subtle quiet-ink subtitle, and an optional "see all" pill.
 *
 * A hand-drawn underline swoosh tucks under the kicker when provided so the
 * section head feels authored instead of stamped. The swoosh inherits the
 * requested [accent] so every section can carry its own identity note
 * (mint for in-progress, butter for remembered, lavender for vault, etc.).
 */
@Composable
fun PSSectionHeader(
    title: String,
    subtitle: String? = null,
    kicker: String? = null,
    accent: androidx.compose.ui.graphics.Color? = null,
    onSeeAll: (() -> Unit)? = null,
    actionLabel: String = "See all",
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    val accentTint = accent ?: pastels.mintDeep
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!kicker.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = kicker,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 0.8.sp,
                        ),
                        color = accentTint,
                    )
                    Spacer(Modifier.width(6.dp))
                    UnderlineSwoosh(color = accentTint.copy(alpha = 0.55f))
                }
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
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

