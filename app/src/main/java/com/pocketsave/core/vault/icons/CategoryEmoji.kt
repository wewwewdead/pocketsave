package com.pocketsave.core.vault.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsave.common.util.ColorOption

/**
 * Emoji counterpart to [AppIcon]. Lets category tiles render a colourful
 * emoji (the same glyph iOS uses) inside a soft-tinted rounded square, so
 * the vault feels like a collection of little stickered cards rather than
 * a list of monochrome Material glyphs.
 *
 * The lookup is intentionally forgiving: when a key isn't in the registry
 * we fall back to a neutral bag emoji instead of a crash. Keys match the
 * existing `iconKey` strings in the database — no migration required.
 */
object CategoryEmoji {
    /** Neutral fallback — "some kind of grocery" in one glyph. */
    const val DEFAULT = "🛍️"

    /** Icon-key → emoji. Keys mirror [AppIcon.registry] so any category
     *  already storing an iconKey resolves to an emoji for free. */
    val registry: Map<String, String> = mapOf(
        // Default GroceryCategory coverage (ordered to match GroceryCategory.kt)
        "fresh_produce" to "🍏",
        "meats_seafood" to "🥩",
        "dairy_eggs" to "🥛",
        "frozen" to "🧊",
        "condiments_ingredients" to "🧂",
        "pantry" to "🥫",
        "bakery_bread" to "🍞",
        "beverages" to "🥤",
        "ready_meals" to "🍱",
        "personal_care" to "🧴",
        "health" to "💊",
        "cleaning_household" to "🧽",
        "pets" to "🐕",
        "baby" to "👶",
        "home_garden" to "🌱",
        "electronics_hobbies" to "🎧",
        "stationery" to "✏️",
        // Picker extras
        "cake" to "🎂",
        "coffee" to "☕",
        "cookie" to "🍪",
        "emoji_food_beverage" to "🍵",
        "fastfood" to "🍔",
        "grass" to "🌿",
        "healing" to "🩹",
        "icecream" to "🍦",
        "liquor" to "🍷",
        "local_cafe" to "☕",
        "local_pizza" to "🍕",
        "lunch_dining" to "🍽️",
        "rice_bowl" to "🍚",
        "recycling" to "♻️",
        "restaurant_menu" to "📖",
        "shopping_basket" to "🧺",
        "wine_bar" to "🍷",
        "label" to "🏷️",
    )

    fun resolve(key: String?): String = key?.let { registry[it] } ?: DEFAULT
}

/**
 * Soft-tinted rounded-square holding an emoji. Matches the iOS vault tile:
 * the square picks up a ~30% alpha of the category's stored colour so the
 * whole row reads as a little collection of stickers. On categories with
 * no explicit colour, the tile falls back to a calm canvas tint.
 */
@Composable
fun CategoryEmojiTile(
    iconKey: String?,
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
    emojiSize: Dp? = null,
    fallbackTint: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val resolvedTint = colorHex
        ?.let { ColorOption.byHex(it)?.color ?: ColorOption.parseHex(it) }
        ?.copy(alpha = 0.35f)
        ?: fallbackTint
    // Emoji font size scales with the tile: roughly 58% of the tile height
    // reads as filling the square without crowding the corners.
    val glyphDp = emojiSize ?: (size * 0.58f)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(resolvedTint),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = CategoryEmoji.resolve(iconKey),
            style = TextStyle(fontSize = glyphDp.value.sp),
        )
    }
}
