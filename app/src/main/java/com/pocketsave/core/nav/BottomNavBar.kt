package com.pocketsave.core.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsave.common.ui.Motion
import com.pocketsave.common.ui.PocketSaveSprings
import com.pocketsave.common.ui.PocketSaveTokens

/**
 * Five-tab bottom nav for the top-level destinations. Active tab shows a
 * soft sage pill behind the icon and a slightly deeper label; inactive tabs
 * read quietly in onSurfaceVariant ink. Tap feedback is a short spring on
 * the icon so the bar feels responsive without going loud.
 */
enum class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Outlined.Home),
    VAULT("vault", "Vault", Icons.Outlined.Inventory2),
    TRIPS("activeTrips", "Trips", Icons.Outlined.ShoppingBag),
    HISTORY("history", "History", Icons.Outlined.HistoryToggleOff),
    MORE("more", "More", Icons.Outlined.MoreHoriz),
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastels = PocketSaveTokens.pastels
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            // Soft hairline above the bar — a whisper of separation from
            // page content without a hard edge.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(pastels.hairline),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavTab.values().forEach { tab ->
                    BottomNavItem(
                        tab = tab,
                        selected = tab.route == currentRoute,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val quietInk = MaterialTheme.colorScheme.onSurfaceVariant

    // Icon "hop" when this tab becomes selected: briefly overshoots to 1.22,
    // then settles back to 1 with a gentler spring. Inactive tabs sit at
    // 0.96 for a subtle quiet-state contrast.
    val iconHop = remember { Animatable(if (selected) 1f else 0.96f) }
    LaunchedEffect(selected) {
        if (selected) {
            iconHop.animateTo(1.22f, animationSpec = PocketSaveSprings.Bouncy)
            iconHop.animateTo(1f, animationSpec = PocketSaveSprings.Gentle)
        } else {
            iconHop.animateTo(0.96f, animationSpec = PocketSaveSprings.Gentle)
        }
    }
    val iconTint by animateColorAsState(
        targetValue = if (selected) primary else quietInk,
        animationSpec = tween(Motion.MediumMs),
        label = "nav-icon-tint",
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) primary else quietInk,
        animationSpec = tween(Motion.MediumMs),
        label = "nav-label-tint",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) container else Color.Transparent,
        animationSpec = tween(Motion.MediumMs),
        label = "nav-pill-tint",
    )
    // Pill grows slightly as it appears behind the icon — same rhythm as
    // the hop so they read as one motion, not two.
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.82f,
        animationSpec = PocketSaveSprings.Gentle,
        label = "nav-pill-scale",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .scale(pillScale)
                .clip(RoundedCornerShape(999.dp))
                .background(pillColor)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconHop.value),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = labelTint,
        )
    }
}
