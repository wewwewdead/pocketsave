package com.pocketsave.core.vault.picker

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketsave.common.util.ColorOption

/**
 * Colour picker composed of two sections:
 *   - A grid of the 16 preset [ColorOption] swatches (same palette used for
 *     cart backgrounds, kept in sync with iOS)
 *   - A collapsible "Custom…" disclosure that reveals three HSV sliders +
 *     a live preview. Dragging a slider deselects any preset swatch.
 *
 * The contract is identical to the old hex-text-field: the caller receives
 * a normalised 6-char uppercase hex via [onHexChange] whenever the user
 * picks a swatch or drags a slider.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchAndHsvPicker(
    selectedHex: String?,
    onHexChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedSelected = selectedHex?.trim()?.removePrefix("#")?.uppercase()?.takeIf { it.length == 6 }
    val matchingSwatch = normalizedSelected?.let { hex -> ColorOption.options.firstOrNull { it.hex == hex } }
    var customExpanded by remember { mutableStateOf(normalizedSelected != null && matchingSwatch == null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorOption.options.forEach { option ->
                SwatchTile(
                    option = option,
                    selected = matchingSwatch?.hex == option.hex,
                    onClick = { onHexChange(option.hex) },
                )
            }
        }

        TextButton(
            onClick = { customExpanded = !customExpanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (customExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text(if (customExpanded) "Hide custom colour" else "Custom…")
        }

        AnimatedVisibility(visible = customExpanded) {
            HsvEditor(
                initialHex = normalizedSelected,
                onHexChange = onHexChange,
            )
        }
    }
}

@Composable
private fun SwatchTile(
    option: ColorOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(option.color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color(0x33000000),
                shape = RoundedCornerShape(10.dp),
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
private fun HsvEditor(
    initialHex: String?,
    onHexChange: (String) -> Unit,
) {
    // Seed the HSV state from the incoming hex so re-opening the editor shows
    // the last chosen colour. Subsequent slider drags update local state; we
    // publish back via `onHexChange` so the parent stays the source of truth.
    val initial = remember(initialHex) { hsvFromHex(initialHex) }
    var hue by remember(initialHex) { mutableStateOf(initial[0]) }
    var saturation by remember(initialHex) { mutableStateOf(initial[1]) }
    var value by remember(initialHex) { mutableStateOf(initial[2]) }

    // Emit an initial hex if the editor was opened without a seed colour, so
    // the preview and the stored value match on first appearance.
    LaunchedEffect(Unit) {
        if (initialHex == null) onHexChange(hsvToHex(hue, saturation, value))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(hsvToColor(hue, saturation, value))
                    .border(1.dp, Color(0x33000000), RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "#${hsvToHex(hue, saturation, value)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SliderRow(label = "Hue", value = hue, range = 0f..360f) { newHue ->
            hue = newHue
            onHexChange(hsvToHex(hue, saturation, value))
        }
        SliderRow(label = "Saturation", value = saturation, range = 0f..1f) { newSat ->
            saturation = newSat
            onHexChange(hsvToHex(hue, saturation, value))
        }
        SliderRow(label = "Value", value = value, range = 0f..1f) { newValue ->
            value = newValue
            onHexChange(hsvToHex(hue, saturation, value))
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(90.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (range.endInclusive <= 1f) "${(value * 100).toInt()}%" else value.toInt().toString(),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.height(24.dp),
        )
    }
}

/** Convert a 6-char hex string to `[hue, saturation, value]`. */
private fun hsvFromHex(hex: String?): FloatArray {
    val cleaned = hex?.trim()?.removePrefix("#")?.uppercase()?.takeIf { it.length == 6 } ?: return DEFAULT_HSV
    val intColor = runCatching { (0xFF000000.toInt()) or cleaned.toLong(16).toInt() }.getOrNull() ?: return DEFAULT_HSV
    val out = FloatArray(3)
    AndroidColor.colorToHSV(intColor, out)
    return out
}

private fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
    val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    val rgb = argb and 0xFFFFFF
    return "%06X".format(rgb)
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color =
    Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))

private val DEFAULT_HSV: FloatArray = floatArrayOf(210f, 0.45f, 0.85f)
