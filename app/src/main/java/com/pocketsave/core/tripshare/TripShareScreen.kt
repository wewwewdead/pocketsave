package com.pocketsave.core.tripshare

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pocketsave.R
import com.pocketsave.common.ui.FuzzyBubblesFamily
import com.pocketsave.common.ui.PocketSaveColors
import com.pocketsave.common.ui.components.DashedLine
import com.pocketsave.common.ui.components.ReceiptGrunge
import com.pocketsave.common.ui.shapes.ReceiptTicketShape
import com.pocketsave.core.service.VaultService
import com.pocketsave.data.prefs.CartBackgroundStore
import com.pocketsave.domain.model.CartStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Port of `PocketSave/Core/TripShare/Views/TripReceiptCardView.swift` preview
 * surface + `TripShareStripView.swift` action strip + the share-sheet
 * plumbing that lives inline on the iOS `CartDetailScreen`.
 *
 * The export bitmap is produced by [TripShareBitmapRenderer]; this screen
 * shows a Compose preview of roughly the same receipt so the user knows what
 * they are about to share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripShareScreen(
    vaultService: VaultService,
    backgroundStore: CartBackgroundStore,
    cartId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: TripShareViewModel = viewModel(
        factory = TripShareViewModel.Factory(
            context = context,
            vaultService = vaultService,
            backgroundStore = backgroundStore,
            cartId = cartId,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val oneShot by viewModel.oneShots.collectAsState()

    LaunchedEffect(oneShot) {
        when (val event = oneShot) {
            is TripShareViewModel.OneShot.Share -> {
                context.startActivity(event.intent)
                viewModel.consumeOneShot()
            }
            is TripShareViewModel.OneShot.Feedback -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                viewModel.consumeOneShot()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.cartDetail.status != CartStatus.COMPLETED) {
                Text(
                    text = "Trip sharing unlocks once the cart is completed.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onBack) { Text("Back") }
                return@Scaffold
            }

            SharePreviewCanvas(state = state)

            val shareFormatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { viewModel.saveToGallery(shareFormatter) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isRendering,
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
                Button(
                    onClick = { viewModel.share(shareFormatter) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isRendering,
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun SharePreviewCanvas(state: TripShareViewModel.UiState) {
    val detail = state.cartDetail

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
            .background(state.backgroundColor.color),
        contentAlignment = Alignment.Center,
    ) {
        if (state.backgroundImageUri != null) {
            AsyncImage(
                model = state.backgroundImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x33000000)))
        }

        ReceiptPreviewCard(detail = detail)
    }
}

@Composable
private fun ReceiptPreviewCard(detail: com.pocketsave.core.cart.CartDetailUiState) {
    val formatter = com.pocketsave.core.currency.LocalCurrencyFormatter.current
    val dateFormatter = remember { SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val captureDate = detail.cart?.completedAt ?: Date()

    // Scalloped paper ticket — iOS TripReceiptTicketShape with 7dp notch + 6dp flat
    // (matches TripReceiptCardView.swift:22).
    val ticketShape = remember { ReceiptTicketShape(notchRadius = 7.dp, flatWidth = 6.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.74f)
            .padding(vertical = 24.dp)
            .clip(ticketShape)
            .background(androidx.compose.ui.graphics.Color.White),
    ) {
        // Subtle paper grunge under the content.
        ReceiptGrunge(Modifier.fillMaxSize())

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "PocketSave",
                fontFamily = FuzzyBubblesFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                color = PocketSaveColors.DarkPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildHeaderTitle(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color(0xBF000000),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            DashedDivider()
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = dateFormatter.format(captureDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color(0x80000000),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = timeFormatter.format(captureDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color(0x80000000),
                )
            }
            Spacer(Modifier.height(8.dp))
            SolidDivider()
            Spacer(Modifier.height(12.dp))
            ReceiptRow(
                label = "Planned Budget",
                value = if (detail.budget > 0.0) formatter.format(detail.budget) else "—",
            )
            Spacer(Modifier.height(4.dp))
            ReceiptRow(label = "Total Spent", value = formatter.format(detail.totalSpent))

            if (detail.budget > 0.0) {
                Spacer(Modifier.height(12.dp))
                DashedDivider()
                Spacer(Modifier.height(8.dp))
                val balance = detail.budget - detail.totalSpent
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color(0x99000000),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = (if (balance >= 0) "+" else "−") + formatter.format(kotlin.math.abs(balance)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) {
                            androidx.compose.ui.graphics.Color.Black
                        } else {
                            androidx.compose.ui.graphics.Color(0xCCAA0000)
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // "thanks" Lottie — plays once when the receipt appears.
            val thanksComposition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.lottie_thanks),
            )
            LottieAnimation(
                composition = thanksComposition,
                iterations = 1,
                modifier = Modifier.height(48.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color(0x99000000),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = androidx.compose.ui.graphics.Color(0xD9000000),
        )
    }
}

@Composable
private fun DashedDivider() {
    DashedLine(color = androidx.compose.ui.graphics.Color(0x2E000000))
}

@Composable
private fun SolidDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(androidx.compose.ui.graphics.Color(0x2E000000)),
    )
}

private fun buildHeaderTitle(detail: com.pocketsave.core.cart.CartDetailUiState): String {
    val stores = detail.sections.map { it.store }.filter { it.isNotBlank() }.distinct()
    val storeStr = stores.take(2).joinToString(" & ")
    val name = detail.name
    return if (storeStr.isEmpty()) name else "$name at $storeStr"
}
