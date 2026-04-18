package com.pocketsave.core.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.pocketsave.MainActivity
import java.text.NumberFormat
import java.util.Locale

/**
 * Port of iOS `PocketSaveWidgets/PurchasedItemsGridWidget.swift`.
 *
 * Loads [PurchasedItemsWidgetSnapshot] from disk (written by
 * [WidgetSnapshotStore]) and renders a compact 4-wide grid of purchased
 * items. Tapping the widget opens [MainActivity]; iOS deep-links to the
 * specific cart, an Android equivalent lands with the deep-link phase.
 */
class PurchasedItemsGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore.readSnapshot(context)
        provideContent { WidgetContent(snapshot) }
    }

    @Composable
    private fun WidgetContent(snapshot: PurchasedItemsWidgetSnapshot?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .background(Color.White)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            when {
                snapshot == null || snapshot.cart == null -> EmptyState()
                snapshot.cart.items.isEmpty() -> EmptyCartState(cartName = snapshot.cart.name)
                else -> CartContent(snapshot.cart)
            }
        }
    }

    @Composable
    private fun CartContent(cart: PurchasedItemsWidgetSnapshot.CartSnapshot) {
        // The snapshot writer pre-formats the total using the user's selected
        // currency; widget providers run outside the Compose tree so
        // `LocalCurrencyFormatter` isn't reachable here. Fall back to a locale
        // formatter when the snapshot predates the `totalSpendingLabel` field.
        val totalLabel = cart.totalSpendingLabel.ifEmpty {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).format(cart.totalSpending)
        }
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier) {
                    Text(
                        text = cart.name,
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFF1F1B1D), night = Color(0xFF1F1B1D)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = "${cart.purchasedCount} of ${cart.totalCount} · $totalLabel",
                        style = TextStyle(
                            color = ColorProvider(day = Color(0x991F1B1D), night = Color(0x991F1B1D)),
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                StatusPill(cart.status)
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            ItemGrid(items = cart.items.take(PurchasedItemsWidgetSnapshot.PAGE_SIZE))
        }
    }

    @Composable
    private fun StatusPill(status: String) {
        Box(
            modifier = GlanceModifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0x1A1F1B1D))
                .cornerRadius(10.dp),
        ) {
            Text(
                text = status,
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF1F1B1D), night = Color(0xFF1F1B1D)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }

    /**
     * 4-wide grid mirroring the iOS `LazyVGrid(columns: 4)` layout. Glance
     * doesn't ship a `LazyVGrid`, so we build rows manually — 20 items fit in
     * 5 rows (matches the iOS `pageSize`).
     */
    @Composable
    private fun ItemGrid(items: List<PurchasedItemsWidgetSnapshot.ItemSnapshot>) {
        val chunked = items.chunked(4)
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            chunked.forEachIndexed { rowIndex, row ->
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    row.forEachIndexed { col, item ->
                        if (col > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                        ItemTile(item = item)
                    }
                    val missing = 4 - row.size
                    repeat(missing) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Box(modifier = GlanceModifier.size(TILE_SIZE)) {}
                    }
                }
                if (rowIndex != chunked.lastIndex) Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }

    @Composable
    private fun ItemTile(item: PurchasedItemsWidgetSnapshot.ItemSnapshot) {
        val context = LocalContext.current
        val bitmap = remember(item.imageUri) { decodeUriToBitmap(context, item.imageUri) }
        val alpha = if (item.isFulfilled) 1f else 0.4f
        Box(
            modifier = GlanceModifier
                .size(TILE_SIZE)
                .background(Color(0x08000000))
                .cornerRadius(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = item.name,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
                    colorFilter = if (!item.isFulfilled) {
                        ColorFilter.tint(ColorProvider(day = Color(0x66000000), night = Color(0x66000000)))
                    } else null,
                )
            } else {
                Text(
                    text = firstChar(item.name),
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0xFF1F1B1D).copy(alpha = alpha),
                            night = Color(0xFF1F1B1D).copy(alpha = alpha),
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }

    @Composable
    private fun EmptyState() {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "No active cart",
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF1F1B1D), night = Color(0xFF1F1B1D)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "Start a shopping trip to see items here.",
                style = TextStyle(
                    color = ColorProvider(day = Color(0x991F1B1D), night = Color(0x991F1B1D)),
                    fontSize = 11.sp,
                ),
            )
        }
    }

    @Composable
    private fun EmptyCartState(cartName: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = cartName,
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF1F1B1D), night = Color(0xFF1F1B1D)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "No purchased items yet.",
                style = TextStyle(
                    color = ColorProvider(day = Color(0x991F1B1D), night = Color(0x991F1B1D)),
                    fontSize = 11.sp,
                ),
            )
        }
    }

    private fun decodeUriToBitmap(context: Context, uriString: String?): android.graphics.Bitmap? {
        if (uriString.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        return runCatching {
            when (uri.scheme) {
                "file" -> uri.path?.let { BitmapFactory.decodeFile(it) }
                else -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        }.getOrNull()
    }

    private fun firstChar(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "?"
        return trimmed.first().uppercase()
    }

    companion object {
        private val TILE_SIZE = 52.dp
    }
}
