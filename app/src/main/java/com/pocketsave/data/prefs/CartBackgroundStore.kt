package com.pocketsave.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

    private fun colorKey(cartId: String) = stringPreferencesKey("cartBackgroundColor_$cartId")
    private fun imageKey(cartId: String) = stringPreferencesKey("cartBackgroundImage_$cartId")

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
}
