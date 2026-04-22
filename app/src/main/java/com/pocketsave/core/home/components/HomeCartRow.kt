package com.pocketsave.core.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pocketsave.R
import com.pocketsave.common.ui.FuzzyBubblesFamily
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.components.BudgetBar
import com.pocketsave.common.ui.components.CartRowGrunge
import com.pocketsave.common.ui.darker
import com.pocketsave.common.ui.saturated
import com.pocketsave.common.util.ColorOption

/**
 * iOS-parity full-width cart card shown in the Active/History tabs on Home.
 *
 * Source of truth: Core/Home/HomeCartRow/Views/HomeCartRowView.swift +
 * HomeCartRowContentCardView.swift + HomeCartRowProgressSectionView.swift.
 *
 * Visual structure (top to bottom):
 *
 *   [Shopping|Planning] chip    … (overflow menu)
 *   ┌─── uneven rounded card (16 top, 24 bottom) ───┐
 *   │  {cart.name}                         {N/M items} │   — FuzzyBubbles 22
 *   │  ─────────────────────────────────────────────── │   — budget bar,
 *   │  category chip · category chip · category chip   │     3-stop gradient
 *   └─────────────────────────────────────────────────┘
 *
 * Background comes from ColorOption.getBackgroundColor(cartId, isRow=true),
 * overlaid with a very faint grunge texture.
 */
data class HomeCartRowItem(
    val cartId: String,
    val name: String,
    val isShopping: Boolean,
    val fulfilledCount: Int,
    val totalItemCount: Int,
    val budget: Double,
    val totalSpent: Double,
    val spentLabel: String,
    val budgetLabel: String?,
    val remainingLabel: String?,
    val overBudget: Boolean,
    val categoryChips: List<CategoryChipDisplay>,
)

data class CategoryChipDisplay(
    val name: String,
    val emoji: String?,
    val tint: Color,
)

@Composable
fun HomeCartRow(
    item: HomeCartRowItem,
    onOpen: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = com.pocketsave.core.haptics.rememberAppHaptics()
    val hapticOpen = {
        haptics.perform(com.pocketsave.core.haptics.AppHaptic.Light)
        onOpen()
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(6.dp)
            .background(
                Color(0xFFF4F4F4).copy(alpha = 0.7f),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp,
                ),
            )
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Header row: status chip + overflow menu.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(isShopping = item.isShopping)
            Spacer(Modifier.weight(1f))
            if (onRename != null || onDelete != null) {
                OverflowMenuButton(onRename = onRename, onDelete = onDelete)
            }
        }

        // Main card.
        CardBody(item = item, onOpen = hapticOpen)
    }
}

@Composable
private fun StatusChip(isShopping: Boolean) {
    Row(
        Modifier.padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isShopping) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.lottie_shoppingmode),
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = if (isShopping) "Shopping" else "Planning",
            fontSize = 11.sp,
            color = PocketSaveColors.DarkPrimary,
            modifier = Modifier.padding(start = if (isShopping) 0.dp else 4.dp),
        )
    }
}

@Composable
private fun OverflowMenuButton(onRename: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "More",
            tint = PocketSaveColors.DarkPrimary,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { expanded = true }
                .padding(8.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            onRename?.let {
                DropdownMenuItem(
                    text = { Text("Rename cart") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = { expanded = false; it() },
                )
            }
            onDelete?.let {
                DropdownMenuItem(
                    text = { Text("Delete cart", color = PocketSaveColors.AccentDanger) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = PocketSaveColors.AccentDanger,
                        )
                    },
                    onClick = { expanded = false; it() },
                )
            }
        }
    }
}

@Composable
private fun CardBody(item: HomeCartRowItem, onOpen: () -> Unit) {
    val bg = ColorOption.getBackgroundColor(item.cartId, isRow = true)
    val borderColor = if (bg == Color.White) {
        Color.Gray.copy(alpha = 0.25f)
    } else {
        bg.darker(0.15f).saturated(0.1f).copy(alpha = 0.6f)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onOpen),
    ) {
        // Faint paper grunge. matchParentSize (not fillMaxSize) so we inherit
        // the card's measured size instead of expanding to the grunge painter's
        // intrinsic dimensions under LazyColumn's infinite height constraint.
        CartRowGrunge(Modifier.matchParentSize())

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Cart name + item count.
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = item.name.ifBlank { "Untitled trip" },
                    fontFamily = FuzzyBubblesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = PocketSaveColors.DarkPrimary,
                    modifier = Modifier.weight(1f),
                )
                ItemCountPill(
                    fulfilled = item.fulfilledCount,
                    total = item.totalItemCount,
                )
            }

            // Budget progress bar.
            if (item.budget > 0) {
                BudgetBar(
                    progress = (item.totalSpent / item.budget).toFloat().coerceIn(0f, 1.2f),
                    overBudget = item.overBudget,
                )
                Row {
                    Text(
                        text = "${item.spentLabel}${if (item.budgetLabel != null) " of ${item.budgetLabel}" else ""}",
                        fontSize = 12.sp,
                        color = PocketSaveColors.DarkPrimary.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.weight(1f))
                    item.remainingLabel?.let { remaining ->
                        Text(
                            text = if (item.overBudget) "−$remaining" else "+$remaining left",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.overBudget) {
                                PocketSaveColors.OverspendAccent
                            } else {
                                PocketSaveColors.SavingsAccent
                            },
                        )
                    }
                }
            } else {
                Text(
                    text = item.spentLabel,
                    fontSize = 13.sp,
                    color = PocketSaveColors.DarkPrimary.copy(alpha = 0.7f),
                )
            }

            // Category chip row.
            if (item.categoryChips.isNotEmpty()) {
                CategoryChipRow(chips = item.categoryChips)
            }
        }
    }
}

@Composable
private fun ItemCountPill(fulfilled: Int, total: Int) {
    val label = if (fulfilled == total) "$total items" else "$fulfilled / $total"
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PocketSaveColors.DarkPrimary,
        )
    }
}

@Composable
private fun CategoryChipRow(chips: List<CategoryChipDisplay>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.take(5).forEach { chip ->
            CategoryChip(chip)
        }
        if (chips.size > 5) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+${chips.size - 5}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = PocketSaveColors.DarkPrimary,
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(chip: CategoryChipDisplay) {
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(chip.tint.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chip.emoji ?: chip.name.take(1),
            fontSize = 13.sp,
        )
    }
}
