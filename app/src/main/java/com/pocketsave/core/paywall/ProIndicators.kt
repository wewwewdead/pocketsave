package com.pocketsave.core.paywall

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.PocketSaveTokens

/**
 * Small "PRO" pill rendered next to an affordance that will bounce a free
 * user to the paywall on tap. Purely informative — the gate itself lives in
 * [com.pocketsave.billing.PaywallGate]. Keeping the visual language in one
 * place here means every lock hint across the app reads identically.
 */
@Composable
fun ProChip(modifier: Modifier = Modifier) {
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = pastels.lavenderSoft,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = pastels.lavenderDeep,
                modifier = Modifier.size(10.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "PRO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = pastels.lavenderDeep,
            )
        }
    }
}

/**
 * One-line, low-pressure cap banner. Renders a soft sage tile with the
 * current cap state on the left and an "Upgrade" affordance on the right.
 * Tapping anywhere on the banner opens the paywall.
 *
 * Callers decide when to render it — this composable never hides itself.
 * The intended usage is "only when the user is actually at the cap", so
 * free users below the limit never see it.
 */
@Composable
fun CapHintBanner(
    label: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    upgradeLabel: String = "Upgrade",
) {
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = pastels.lavenderSoft,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onUpgrade),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = pastels.lavenderDeep,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = pastels.lavenderDeep,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = upgradeLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = pastels.lavenderDeep,
            )
        }
    }
}
