package com.pocketsave.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.AppShapes
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.common.ui.decor.blobDecor
import com.pocketsave.common.ui.decor.grainOverlay

/**
 * Warm empty state. Replaces every bare "Your X live here." placeholder in
 * the app with a small editorial moment: a blob-tinted sticker, an
 * italic headline, a gentle body line, and optional inline CTA slot.
 *
 * Keeps its composition small so it can slot into a card, a section, or a
 * full-screen space without redesign.
 */
@Composable
fun AffectionateEmpty(
    title: String,
    body: String,
    icon: ImageVector,
    accent: Color? = null,
    modifier: Modifier = Modifier,
    cta: @Composable (() -> Unit)? = null,
) {
    val pastels = PocketSaveTokens.pastels
    val iconTint = accent ?: pastels.mintDeep
    val blobTint = (accent ?: pastels.mintDeep).copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.HeroCard)
            .background(MaterialTheme.colorScheme.surface)
            .grainOverlay(tint = pastels.grain, density = 0.7f)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .blobDecor(color = blobTint, seed = title.hashCode()),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (cta != null) {
            Spacer(Modifier.height(16.dp))
            cta()
        }
    }
}
