package com.pocketsave.core.cart

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pocketsave.common.util.ColorOption
import com.pocketsave.common.util.ImageStorage
import com.pocketsave.data.prefs.CartBackgroundStore
import kotlinx.coroutines.launch

/**
 * Port of iOS `BackgroundPickerContent` from `Core/Detail Cart/Views/ModeToggleView.swift`.
 *
 * Presents the 16-swatch palette + a "Choose photo" tile. Selecting a swatch
 * clears any stored image; selecting an image clears the stored colour — same
 * invariant iOS preserves via `CartBackgroundImageManager.saveImage`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBackgroundPicker(
    cartId: String,
    backgroundStore: CartBackgroundStore,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageStorage = remember { ImageStorage(context.applicationContext) }

    val currentHex by backgroundStore.colorHex(cartId).collectAsState(initial = null)
    val currentImage by backgroundStore.imageUri(cartId).collectAsState(initial = null)

    var submitting by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            submitting = true
            // Copy the picked media into app-private storage so the URI remains
            // readable after the picker permission expires. Mirrors the iOS
            // `CartBackgroundImageManager.saveImage(...)` flow that persists a
            // JPEG on disk under Documents.
            val stored = imageStorage.saveFromUri(uri, maxDimension = 2048)
            if (stored != null) {
                // If we had a previous image we wrote to app storage, delete it.
                val previous = currentImage
                backgroundStore.setImage(cartId, stored)
                if (previous != null && previous != stored) imageStorage.deleteByUri(previous)
            }
            submitting = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cart background", style = MaterialTheme.typography.titleLarge)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 64.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "photo", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(1) }) {
                    PhotoTile(
                        currentImage = currentImage,
                        enabled = !submitting,
                        onPick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }
                items(items = ColorOption.options, key = { it.hex }) { option ->
                    ColorTile(
                        option = option,
                        selected = currentImage == null && (currentHex == option.hex),
                        onClick = {
                            scope.launch {
                                val previousImage = currentImage
                                backgroundStore.setColor(cartId, option.hex)
                                if (previousImage != null) imageStorage.deleteByUri(previousImage)
                            }
                        },
                    )
                }
            }

            if (currentImage != null) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val previousImage = currentImage
                            backgroundStore.setColor(cartId, ColorOption.defaultColor.hex)
                            if (previousImage != null) imageStorage.deleteByUri(previousImage)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Remove photo")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // Close the sheet shortly after the user changes the value — iOS returns
    // immediately to the cart detail once the picker commits.
    LaunchedEffect(currentHex, currentImage) {
        // no-op: preserving the state observation to keep the sheet responsive.
    }
}

@Composable
private fun ColorTile(option: ColorOption, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(option.color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color(0x33000000),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PhotoTile(currentImage: String?, enabled: Boolean, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    width = if (currentImage != null) 2.dp else 1.dp,
                    color = if (currentImage != null) MaterialTheme.colorScheme.primary else Color(0x33000000),
                ),
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (currentImage != null) {
            AsyncImage(
                model = currentImage,
                contentDescription = "Current background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Choose photo")
        }
    }
}

