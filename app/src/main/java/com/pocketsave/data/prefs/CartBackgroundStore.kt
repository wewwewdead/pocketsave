package com.pocketsave.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cartBackgroundDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cart_backgrounds",
)

/**
 * Port of the iOS cart-background persistence mix:
 *   - `UserDefaults.standard.string(forKey: "cartBackgroundColor_<cartId>")`
 *   - `CartBackgroundImageManager.shared` (file on disk under Documents)
 *
 * Android collapses both into a single DataStore so the two keys live and die
 * together — setting an image clears the colour, setting a colour deletes the
 * image file. Image bytes are still stored off-row (file path persisted here,
 * bytes on disk via the shared [com.pocketsave.common.util.ImageStorage]).
 */
class CartBackgroundStore(private val context: Context) {

    private fun colorKey(cartId: String) = stringPreferencesKey("$COLOR_PREFIX$cartId")
    private fun imageKey(cartId: String) = stringPreferencesKey("$IMAGE_PREFIX$cartId")

    /**
     * Observable colour hex for the cart. Returns null when no custom colour is
     * stored (caller should fall back to [com.pocketsave.common.util.ColorOption.defaultColor]).
     */
    fun colorHex(cartId: String): Flow<String?> =
        context.cartBackgroundDataStore.data.map { it[colorKey(cartId)] }

    fun imageUri(cartId: String): Flow<String?> =
        context.cartBackgroundDataStore.data.map { it[imageKey(cartId)] }

    suspend fun colorHexNow(cartId: String): String? = colorHex(cartId).first()

    suspend fun imageUriNow(cartId: String): String? = imageUri(cartId).first()

    /**
     * Bulk observation of every cart's stored background. Lets list screens
     * (home, history) collect a single flow rather than one pair of flows per
     * row, matching the iOS pattern where the background dictionary is read
     * from a single `UserDefaults` snapshot + in-memory image cache.
     *
     * The map is keyed by `cartId` and only contains carts that have at least
     * one of `colorHex` or `imageUri` stored — absent carts inherit the
     * theme-default surface background. Emits on every DataStore write; we
     * apply `distinctUntilChanged` so equal maps don't force recompositions.
     */
    val allBackgrounds: Flow<Map<String, CartBackground>> =
        context.cartBackgroundDataStore.data
            .map { prefs ->
                val bucket = HashMap<String, CartBackground>()
                prefs.asMap().forEach { (key, raw) ->
                    val value = raw as? String ?: return@forEach
                    val name = key.name
                    val cartId = when {
                        name.startsWith(COLOR_PREFIX) -> name.removePrefix(COLOR_PREFIX)
                        name.startsWith(IMAGE_PREFIX) -> name.removePrefix(IMAGE_PREFIX)
                        else -> return@forEach
                    }
                    val prior = bucket[cartId] ?: CartBackground.EMPTY
                    bucket[cartId] = if (name.startsWith(COLOR_PREFIX)) {
                        prior.copy(colorHex = value)
                    } else {
                        prior.copy(imageUri = value)
                    }
                }
                bucket
            }
            .distinctUntilChanged()

    /**
     * Port of iOS `CartBackgroundImageManager.saveImage(... forCartId:)`: saving
     * an image wins over the colour preference.
     */
    suspend fun setImage(cartId: String, uri: String) {
        context.cartBackgroundDataStore.edit {
            it[imageKey(cartId)] = uri
            it.remove(colorKey(cartId))
        }
    }

    suspend fun setColor(cartId: String, hex: String) {
        context.cartBackgroundDataStore.edit {
            it[colorKey(cartId)] = hex.trim().removePrefix("#").uppercase()
            it.remove(imageKey(cartId))
        }
    }

    /**
     * Port of `CartBackgroundImageManager.deleteImage(forCartId:)` combined with
     * `UserDefaults.removeObject(forKey: "cartBackgroundColor_<cartId>")`. Used
     * when a cart is hard-deleted — mirrors iOS `deleteCart`.
     */
    suspend fun clear(cartId: String) {
        context.cartBackgroundDataStore.edit {
            it.remove(colorKey(cartId))
            it.remove(imageKey(cartId))
        }
    }

    /**
     * Wipes every stored per-cart background entry. Used by the Reset-App flow
     * — after the carts themselves are deleted there's nothing these keys can
     * legitimately refer to anyway.
     */
    suspend fun clearAll() {
        context.cartBackgroundDataStore.edit { it.clear() }
    }

    companion object {
        private const val COLOR_PREFIX = "cartBackgroundColor_"
        private const val IMAGE_PREFIX = "cartBackgroundImage_"
    }
}

/**
 * Combined per-cart background record. Either field may be null; when both are
 * null the row uses the theme default. Matching invariant: only one of the two
 * can be set at a time (see [CartBackgroundStore.setColor] / [setImage]), so in
 * practice a populated record has exactly one non-null field.
 *
 * Marked `@Immutable` so Compose skips `CartSummaryCard` / `TripRow`
 * recomposition when the per-cart record is structurally equal across emissions.
 */
@androidx.compose.runtime.Immutable
data class CartBackground(
    val colorHex: String? = null,
    val imageUri: String? = null,
) {
    companion object {
        val EMPTY = CartBackground()
    }
}
