package com.pocketsave.common.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pocketsave.R
import com.pocketsave.common.ui.PocketSaveColors

/**
 * Subtle paper-grunge texture overlay. iOS reference: Components/GrungeTextureView.swift.
 *
 * Used on:
 *  - Cart row backgrounds (very faint, ~5% alpha, multiply blend)
 *  - Receipt/ticket shapes (Vault item detail, Trip Share)
 *  - Binder sidebar
 *
 * Place inside a `Box` stack on top of the colored background, clipped to the
 * same shape as the underlying surface.
 */
@Composable
fun GrungeTexture(
    modifier: Modifier = Modifier,
    alpha: Float = 0.05f,
    tint: Color? = null,
) {
    Image(
        painter = painterResource(R.drawable.grunge),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = alpha,
        colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.Multiply) },
        modifier = modifier.fillMaxSize(),
    )
}

/** Preset for cart row backgrounds — very subtle dark multiply. */
@Composable
fun CartRowGrunge(modifier: Modifier = Modifier) {
    GrungeTexture(
        modifier = modifier,
        alpha = 0.06f,
        tint = PocketSaveColors.DarkPrimary,
    )
}

/** Preset for paper receipts — slightly more visible, untinted. */
@Composable
fun ReceiptGrunge(modifier: Modifier = Modifier) {
    GrungeTexture(modifier = modifier, alpha = 0.08f)
}
