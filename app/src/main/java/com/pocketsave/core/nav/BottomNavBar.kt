package com.pocketsave.core.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
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
import com.pocketsave.common.ui.PastelPalette
import com.pocketsave.common.ui.PocketSaveSprings
import com.pocketsave.common.ui.PocketSaveTokens

/**
 * Five-tab dock. Each tab carries its own identity hue, surfaced when
 * selected — Home sits in the sage CTA family, Vault in lavender-dusk,
 * Trips in mint, History in peach, More in butter. The pill behind the
 * icon animates in with a gentle scale + colour tween, and the icon itself
 * hops a touch on selection for a small "I got you" beat.
 *
 * The bar floats with rounded top corners and a soft shadow so it reads
 * like a companion dock rather than a system strip.
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

private data class TabTint(val soft: Color, val deep: Color)

/** Every tab resolves to the same sage pill — the bar is monotone. The
 *  animation (pill scale + icon hop + dot indicator) carries the identity
 *  between tabs instead of colour. */
@Composable
private fun NavTab.tint(pastels: PastelPalette, primary: Color): TabTint =
    TabTint(soft = pastels.mintSoft, deep = primary)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
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

@Composable
private fun BottomNavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pastels = PocketSaveTokens.pastels
    val primary = MaterialTheme.colorScheme.primary
    val quietInk = MaterialTheme.colorScheme.onSurfaceVariant
    val tabTint = tab.tint(pastels, primary)

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
        targetValue = if (selected) tabTint.deep else quietInk,
        animationSpec = tween(Motion.MediumMs),
        label = "nav-icon-tint",
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) tabTint.deep else quietInk,
        animationSpec = tween(Motion.MediumMs),
        label = "nav-label-tint",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) tabTint.soft else Color.Transparent,
        animationSpec = tween(Motion.MediumMs),
        label = "nav-pill-tint",
    )
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.8f,
        animationSpec = PocketSaveSprings.Gentle,
        label = "nav-pill-scale",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(Motion.MediumMs),
        label = "nav-dot",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
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
                .padding(horizontal = 16.dp, vertical = 7.dp),
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
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = labelTint,
        )
        // Soft identity dot beneath the label — a small "this is me" mark
        // that only renders for the selected tab. Inherits the tab's deep
        // tint so the nav feels alive without adding visual noise.
        Box(
            modifier = Modifier
                .size(3.dp)
                .scale(indicatorAlpha)
                .clip(CircleShape)
                .background(tabTint.deep),
        )
    }
}
