package com.pocketsave.core.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.currency.CurrencyPickerSheet
import com.pocketsave.core.currency.LocalCurrencyFormatter
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.pocketsave.core.home.pressScale
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.prefs.AppPreferences
import kotlinx.coroutines.launch

/**
 * The More tab. Hosts the previously-sheet-based settings surface:
 * identity, currency, trash, and the destructive reset action.
 *
 * Lives as a top-level tab (no back button) so the bottom nav is the only
 * navigator users need. `onOpenTrash` and `onResetCompleted` push the user
 * elsewhere when selected; everything else handles itself inline.
 */
// Matches the seed name VaultService writes for a fresh install. Kept here
// so MoreScreen can treat it as "unset" without depending on a shared
// constant from the data brain.
private const val DEFAULT_SEED_NAME = "Default User"

@Composable
fun MoreScreen(
    vaultService: VaultService,
    preferences: AppPreferences,
    subscriptionManager: SubscriptionManager,
    onOpenTrash: () -> Unit,
    onOpenPaywall: () -> Unit,
    onResetApp: suspend () -> Unit,
    onResetCompleted: () -> Unit,
) {
    val state by vaultService.state.collectAsState()
    val formatter = LocalCurrencyFormatter.current
    // Treat the database seed name ("Default User") as unset so first-run
    // users see the friendlier placeholder and the edit sheet opens empty.
    val storedName = state.user?.name
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != DEFAULT_SEED_NAME }
    val userName = storedName ?: "PocketSave user"
    val isPro by subscriptionManager.isPro.collectAsState()

    val selectedCurrencyCode by preferences.selectedCurrencyCode.collectAsState(initial = null)
    val selectedCurrencySymbol by preferences.selectedCurrencySymbol.collectAsState(initial = null)
    val currencyLabel = buildString {
        val code = selectedCurrencyCode ?: formatter.preference.code
        val symbol = selectedCurrencySymbol?.takeIf { it.isNotBlank() } ?: formatter.preference.symbol
        append(code)
        if (symbol.isNotBlank()) {
            append(" · "); append(symbol)
        }
    }

    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showNameSheet by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    val pastels = PocketSaveTokens.pastels

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "More",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Profile, preferences, and app data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(22.dp))

            // Identity card. Tapping opens a small sheet to edit the display
            // name — persists to Room via `VaultService.updateUserName`, which
            // already trims + rejects blanks so the rest of the app can rely
            // on `user.name` being non-empty once the user has set one.
            val identityInteraction = remember { MutableInteractionSource() }
            Surface(
                color = pastels.mintSoft,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(identityInteraction)
                    .clickable(
                        interactionSource = identityInteraction,
                        indication = null,
                        onClick = { showNameSheet = true },
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBubble(
                        icon = Icons.Outlined.Person,
                        tint = Color.White.copy(alpha = 0.75f),
                        iconTint = pastels.mintDeep,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SIGNED IN AS",
                            style = MaterialTheme.typography.labelSmall,
                            color = pastels.mintDeep.copy(alpha = 0.78f),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = pastels.mintDeep,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = pastels.mintDeep,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            ProMembershipRow(
                isPro = isPro,
                onClick = onOpenPaywall,
            )

            Spacer(Modifier.height(14.dp))

            SectionLabel(text = "Preferences")
            Spacer(Modifier.height(8.dp))
            MoreRow(
                title = "Currency",
                subtitle = currencyLabel,
                icon = Icons.Outlined.Payments,
                onClick = { showCurrencyPicker = true },
            )
            Spacer(Modifier.height(10.dp))
            MoreRow(
                title = "Trash",
                subtitle = "Restore completed trips you deleted",
                icon = Icons.Outlined.DeleteOutline,
                onClick = onOpenTrash,
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel(text = "Danger zone", tintWarning = true)
            Spacer(Modifier.height(8.dp))
            ResetRow(
                isResetting = isResetting,
                onClick = { showResetConfirmation = true },
            )

            Spacer(Modifier.weight(1f))
            Text(
                text = "PocketSave",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            preferences = preferences,
            onDismiss = { showCurrencyPicker = false },
        )
    }

    if (showNameSheet) {
        EditNameSheet(
            initial = storedName.orEmpty(),
            onDismiss = { showNameSheet = false },
            onSubmit = { newName ->
                scope.launch {
                    vaultService.updateUserName(newName)
                    showNameSheet = false
                    haptics.perform(AppHaptic.Confirm)
                }
            },
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    // Reject on destructive confirm — same tone as perm-delete.
                    // The Confirm below fires only if the wipe actually succeeds.
                    haptics.perform(AppHaptic.Reject)
                    scope.launch {
                        isResetting = true
                        try {
                            onResetApp()
                            haptics.perform(AppHaptic.Confirm)
                            onResetCompleted()
                        } catch (t: Throwable) {
                            android.util.Log.e("PocketSave", "Reset failed", t)
                        } finally {
                            isResetting = false
                        }
                    }
                }) {
                    Text(
                        text = "Reset everything",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            },
            title = {
                Text(
                    text = "Reset app?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            },
            text = {
                Text(
                    text = "This permanently deletes every trip, vault item, " +
                        "category, saved price, and preference. You'll go back " +
                        "through onboarding. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun SectionLabel(text: String, tintWarning: Boolean = false) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (tintWarning) MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MoreRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBubble(
                icon = icon,
                tint = pastels.mintSoft,
                iconTint = pastels.mintDeep,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ResetRow(
    isResetting: Boolean,
    onClick: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = pastels.blushSoft,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.985f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !isResetting,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    tint = pastels.blushDeep,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isResetting) "Resetting…" else "Reset app",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = pastels.blushDeep,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Wipes all trips, items, and preferences.",
                    style = MaterialTheme.typography.labelSmall,
                    color = pastels.blushDeep.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color, iconTint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Bottom sheet for editing the display name. Backed by
 * [com.pocketsave.core.service.VaultService.updateUserName], which trims the
 * input and rejects blanks — so the submit button stays disabled until the
 * user has actually typed something.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditNameSheet(
    initial: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial) }
    val trimmed = name.trim()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Your name",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "Shown on the More tab and on shared trip receipts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onSubmit(trimmed) },
                    enabled = trimmed.isNotEmpty() && trimmed != initial.trim(),
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * "PocketSave Pro" entry card. Doubles as the current-status indicator and
 * the entry point into [com.pocketsave.core.paywall.PaywallScreen]. When the
 * user is already Pro, the copy flips to confirmation + a discreet "Manage"
 * affordance via the same paywall screen (which also hosts Restore).
 */
@Composable
private fun ProMembershipRow(
    isPro: Boolean,
    onClick: () -> Unit,
) {
    val pastels = PocketSaveTokens.pastels
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = pastels.lavenderSoft,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBubble(
                icon = Icons.Filled.AutoAwesome,
                tint = Color.White.copy(alpha = 0.7f),
                iconTint = pastels.lavenderDeep,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "POCKETSAVE PRO",
                    style = MaterialTheme.typography.labelSmall,
                    color = pastels.lavenderDeep.copy(alpha = 0.78f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isPro) "Membership active" else "Unlock the full app",
                    style = MaterialTheme.typography.titleMedium
                        .copy(fontWeight = FontWeight.SemiBold),
                    color = pastels.lavenderDeep,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isPro)
                        "Manage your plan or restore purchases."
                    else
                        "Unlimited trips, scanning, widgets, insights, and more.",
                    style = MaterialTheme.typography.labelSmall,
                    color = pastels.lavenderDeep.copy(alpha = 0.78f),
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = pastels.lavenderDeep,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
