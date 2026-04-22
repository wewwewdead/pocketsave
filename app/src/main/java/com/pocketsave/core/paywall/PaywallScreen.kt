package com.pocketsave.core.paywall

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketsave.billing.BillingAnalytics
import com.pocketsave.billing.NoOpBillingAnalytics
import com.pocketsave.billing.PremiumFeature
import com.pocketsave.billing.SubscriptionManager
import com.pocketsave.common.ui.PastelPalette
import com.pocketsave.common.ui.PocketSaveTokens
import com.pocketsave.core.haptics.AppHaptic
import com.pocketsave.core.haptics.rememberAppHaptics
import com.revenuecat.purchases.Package
import kotlinx.coroutines.delay

/**
 * PocketSave Pro paywall. Presented as a standalone nav destination
 * (`Routes.PAYWALL`) so it can be opened from settings now and from
 * feature-level gates in later phases. Never shown on launch; never shown
 * during onboarding.
 *
 * Design notes:
 *  - Single accent family (sage) inherited from [PocketSaveTokens.pastels] so
 *    the paywall reads like part of the app rather than a bolted-on upsell.
 *  - Selection defaults to the yearly plan when both are available. The
 *    yearly card carries a "Best value" pill and the savings-vs-monthly
 *    percentage when we can compute it off the Play price.
 *  - Success / cancel / error all land in the snackbar and auto-dismiss the
 *    screen on a real purchase so the user immediately lands back where
 *    they came from, now Pro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    subscriptionManager: SubscriptionManager,
    onDismiss: () -> Unit,
    trigger: PremiumFeature? = null,
    analytics: BillingAnalytics = NoOpBillingAnalytics,
) {
    val viewModel: PaywallViewModel = viewModel(
        factory = PaywallViewModel.Factory(
            subscriptionManager = subscriptionManager,
            analytics = analytics,
            trigger = trigger,
        ),
    )
    val ui by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarHost = remember { SnackbarHostState() }
    val haptics = rememberAppHaptics()
    val pastels = PocketSaveTokens.pastels

    // Purchase lifecycle: flash a welcome and bounce back to the caller so
    // the user lands in the flow they originally tried to use, now with Pro.
    LaunchedEffect(ui.purchaseCompleted) {
        if (ui.purchaseCompleted) {
            haptics.perform(AppHaptic.Confirm)
            snackbarHost.showSnackbar("Welcome to PocketSave Pro!")
            delay(600)
            onDismiss()
        }
    }

    // Restore lifecycle. "Restored" dismisses; "no subscription" stays on
    // screen with a calm explanation so the user can still try a fresh
    // purchase if that's what they actually meant to do.
    LaunchedEffect(ui.restoreResult) {
        when (ui.restoreResult) {
            PaywallViewModel.RestoreOutcome.Restored -> {
                haptics.perform(AppHaptic.Confirm)
                snackbarHost.showSnackbar("Subscription restored.")
                delay(500)
                onDismiss()
            }
            PaywallViewModel.RestoreOutcome.NoActiveSubscription -> {
                haptics.perform(AppHaptic.Light)
                snackbarHost.showSnackbar(
                    "No active PocketSave Pro subscription on this Google account.",
                )
                viewModel.consumeRestoreResult()
            }
            null -> Unit
        }
    }

    // Errors ride the same snackbar but with a reject buzz so the feedback
    // matches existing "blocked action" moments in the app.
    LaunchedEffect(ui.error) {
        val message = ui.error
        if (message != null) {
            haptics.perform(AppHaptic.Reject)
            snackbarHost.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HeroSection(
                isAlreadyPro = ui.isPro,
                triggerSubtitle = trigger?.copy?.heroSubtitle,
                pastels = pastels,
            )

            BenefitsCard(pastels = pastels)

            if (ui.isPro) {
                // Already-subscribed state: no plan selector, no purchase CTA.
                // The canonical action is "Manage subscription", which deep-
                // links to the Play Store's subscription page scoped to this
                // app. Restore stays available for users signed into Play
                // with a different account than the one that bought Pro.
                ManageSubscriptionButton(
                    onClick = {
                        val host = activity ?: context
                        haptics.perform(AppHaptic.Light)
                        openPlayStoreSubscriptions(host)
                    },
                )
            } else {
                when {
                    ui.isLoading -> PricingLoadingCard(pastels = pastels)

                    ui.monthlyPackage == null && ui.yearlyPackage == null ->
                        PricingUnavailableCard(
                            isConfigured = ui.isConfigured,
                            onRetry = { subscriptionManager.refresh() },
                            pastels = pastels,
                        )

                    else -> PackageSelector(
                        monthly = ui.monthlyPackage,
                        yearly = ui.yearlyPackage,
                        selected = ui.selectedPackage,
                        onSelect = viewModel::selectPackage,
                        pastels = pastels,
                    )
                }

                PrimaryCta(
                    label = if (ui.isPurchasing) "Starting…" else "Start PocketSave Pro",
                    enabled = ui.selectedPackage != null &&
                        !ui.isPurchasing &&
                        activity != null,
                    isBusy = ui.isPurchasing,
                    onClick = {
                        val host = activity ?: return@PrimaryCta
                        haptics.perform(AppHaptic.Light)
                        viewModel.purchaseSelected(host)
                    },
                )

                FinePrint()
            }

            RestoreRow(
                isBusy = ui.isRestoring,
                onClick = {
                    haptics.perform(AppHaptic.Light)
                    viewModel.restorePurchases()
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sections
// ---------------------------------------------------------------------------

@Composable
private fun HeroSection(
    isAlreadyPro: Boolean,
    triggerSubtitle: String?,
    pastels: PastelPalette,
) {
    val subtitle = when {
        isAlreadyPro -> "Thanks for supporting PocketSave."
        // Trigger-specific copy wins when the paywall was opened from a
        // feature gate — keeps the reason the user is here front-and-centre.
        !triggerSubtitle.isNullOrBlank() -> triggerSubtitle
        else -> "Plan smarter. Shop calmer. Spend a little less."
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(pastels.mintSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = pastels.mintDeep,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (isAlreadyPro) "You're on Pro" else "PocketSave Pro",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BenefitsCard(pastels: PastelPalette) {
    val items = listOf(
        BenefitLine(
            icon = Icons.Outlined.AllInclusive,
            title = "Unlimited trips",
            body = "Plan as many shopping trips as you need, at once.",
        ),
        BenefitLine(
            icon = Icons.Outlined.Inventory2,
            title = "A vault without walls",
            body = "Save every item, store, and category your pantry uses.",
        ),
        BenefitLine(
            icon = Icons.Outlined.QrCodeScanner,
            title = "Scan, don't type",
            body = "Point your camera at a label — PocketSave fills in the name, price, and size.",
        ),
        BenefitLine(
            icon = Icons.Outlined.BarChart,
            title = "See your spending clearly",
            body = "Full trip history, budget deltas, and category insights.",
        ),
        BenefitLine(
            icon = Icons.Outlined.Widgets,
            title = "Home-screen widget",
            body = "Watch your cart fill in real time, right from the home screen.",
        ),
        BenefitLine(
            icon = Icons.Outlined.Palette,
            title = "Make it yours",
            body = "Cart themes, photo backgrounds, and polished share cards.",
        ),
    )

    Surface(
        color = pastels.mintSoft,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items.forEach { BenefitRow(line = it, pastels = pastels) }
        }
    }
}

@Composable
private fun BenefitRow(line: BenefitLine, pastels: PastelPalette) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = line.icon,
                contentDescription = null,
                tint = pastels.mintDeep,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.title,
                style = MaterialTheme.typography.titleMedium
                    .copy(fontWeight = FontWeight.SemiBold),
                color = pastels.mintDeep,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = line.body,
                style = MaterialTheme.typography.bodyMedium,
                color = pastels.mintDeep.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun PackageSelector(
    monthly: Package?,
    yearly: Package?,
    selected: Package?,
    onSelect: (Package) -> Unit,
    pastels: PastelPalette,
) {
    val savingsPct = computeYearlySavingsPercent(monthly = monthly, yearly = yearly)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (monthly != null) {
            PlanCard(
                title = "Monthly",
                priceLabel = monthly.price(),
                caption = "Billed every month.",
                selected = selected?.identifier == monthly.identifier,
                badge = null,
                onClick = { onSelect(monthly) },
                pastels = pastels,
            )
        }
        if (yearly != null) {
            PlanCard(
                title = "Yearly",
                priceLabel = yearly.price(),
                caption = buildString {
                    append("Billed once a year.")
                    if (savingsPct != null && savingsPct >= 5) {
                        append(" Save about $savingsPct% vs monthly.")
                    }
                },
                selected = selected?.identifier == yearly.identifier,
                badge = "Best value",
                onClick = { onSelect(yearly) },
                pastels = pastels,
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    priceLabel: String,
    caption: String,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    pastels: PastelPalette,
) {
    val borderColor = if (selected) pastels.mintDeep else pastels.hairline
    val backgroundColor = if (selected) pastels.mintSoft else MaterialTheme.colorScheme.surface
    val interaction = remember { MutableInteractionSource() }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            )
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
            RadioDot(selected = selected, tint = pastels.mintDeep)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                            .copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        BadgePill(text = badge, pastels = pastels)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = priceLabel,
                style = MaterialTheme.typography.titleMedium
                    .copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean, tint: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = tint, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tint),
            )
        }
    }
}

@Composable
private fun BadgePill(text: String, pastels: PastelPalette) {
    Surface(
        color = pastels.lavenderSoft,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
                .copy(fontWeight = FontWeight.SemiBold),
            color = pastels.lavenderDeep,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun PrimaryCta(
    label: String,
    enabled: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium
                .copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun FinePrint() {
    Text(
        text = "Subscriptions renew automatically until cancelled. Manage or " +
            "cancel anytime in the Google Play Store.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RestoreRow(
    isBusy: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = if (isBusy) "Restoring…" else "Restore purchases",
            style = MaterialTheme.typography.titleSmall
                .copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun PricingLoadingCard(pastels: PastelPalette) {
    Surface(
        color = pastels.canvasTint,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                color = pastels.mintDeep,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Loading plans…",
                style = MaterialTheme.typography.bodyMedium,
                color = pastels.mintDeep,
            )
        }
    }
}

@Composable
private fun PricingUnavailableCard(
    isConfigured: Boolean,
    onRetry: () -> Unit,
    pastels: PastelPalette,
) {
    Surface(
        color = pastels.canvasTint,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Pricing unavailable",
                style = MaterialTheme.typography.titleMedium
                    .copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isConfigured)
                    "We couldn't reach the store. Check your connection and try again."
                else
                    "Subscriptions aren't available on this build yet. Please check back soon.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isConfigured) {
                Spacer(Modifier.height(2.dp))
                TextButton(onClick = onRetry) {
                    Text(
                        text = "Try again",
                        style = MaterialTheme.typography.titleSmall
                            .copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private data class BenefitLine(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

/**
 * Pretty-prints the store-formatted price for the package. Falls back to the
 * raw amount if the SDK can't produce a localised string, which can happen
 * when Play Billing is unreachable (offline) but a cached package still
 * surfaces.
 */
private fun Package.price(): String {
    val formatted = product.price.formatted
    if (formatted.isNotBlank()) return formatted
    return product.price.amountMicros.let { micros ->
        val whole = micros / 1_000_000.0
        "%.2f %s".format(whole, product.price.currencyCode)
    }
}

/**
 * Yearly savings vs 12 × monthly, rounded to a whole percent. Returns `null`
 * when either package is missing or the amounts aren't usable. Consumer
 * suppresses display under 5% so sub-noise doesn't read as a fake discount.
 */
private fun computeYearlySavingsPercent(monthly: Package?, yearly: Package?): Int? {
    val m = monthly?.product?.price?.amountMicros ?: return null
    val y = yearly?.product?.price?.amountMicros ?: return null
    if (m <= 0L || y <= 0L) return null
    val twelveMonthsOfMonthly = m.toDouble() * 12.0
    if (twelveMonthsOfMonthly <= 0.0) return null
    val savings = (twelveMonthsOfMonthly - y.toDouble()) / twelveMonthsOfMonthly
    if (savings <= 0.0) return null
    return (savings * 100.0).toInt()
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Full-width "Manage subscription" button shown when the current user is
 * already Pro. Paired with [openPlayStoreSubscriptions] — Google Play is the
 * only place Android subscriptions can be modified per Play policy, so this
 * is the entire surface the already-subscribed path needs.
 */
@Composable
private fun ManageSubscriptionButton(onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = "Manage subscription in Play Store",
            style = MaterialTheme.typography.titleMedium
                .copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * Deep-links into the Play Store's subscription page scoped to this app.
 * The URL form works whether or not the Play Store app is installed — the
 * system resolves `https://play.google.com/...` to the Play app when
 * available, or the browser as a fallback. If neither resolves, the intent
 * is swallowed silently; the paywall still exposes Restore so the user has
 * a recovery path.
 */
private fun openPlayStoreSubscriptions(context: Context) {
    val uri = Uri.parse(
        "https://play.google.com/store/account/subscriptions?package=${context.packageName}",
    )
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (t: ActivityNotFoundException) {
        Log.w("Paywall", "No handler for Play subscriptions URI", t)
    }
}
