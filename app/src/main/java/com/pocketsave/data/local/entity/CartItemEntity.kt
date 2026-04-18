package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Mirrors the iOS `CartItem` type.
 *
 * `itemId` is NOT a hard foreign key because shopping-only cart items synthesise
 * a fake `itemId` that never hits the vault (see `CartItem.createShoppingOnlyItem`
 * in iOS). We only FK to the owning cart.
 *
 * `shoppingOnlyImageUri` replaces `shoppingOnlyImageData` — image bytes live on
 * disk under the app-private storage.
 */
@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = CartEntity::class,
            parentColumns = ["id"],
            childColumns = ["cartId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("cartId"),
        Index("itemId"),
    ],
)
data class CartItemEntity(
    @PrimaryKey val uid: String,
    val cartId: String,
    val itemId: String,
    val addedAt: Date? = null,
    val quantity: Double,
    val isFulfilled: Boolean = false,
    val isSkippedDuringShopping: Boolean = false,

    // Planned
    val plannedStore: String,
    val plannedPrice: Double? = null,
    val plannedUnit: String? = null,

    // Actual
    val actualStore: String? = null,
    val actualPrice: Double? = null,
    val actualQuantity: Double? = null,
    val actualUnit: String? = null,
    val wasEditedDuringShopping: Boolean = false,

    // Shopping-only
    val isShoppingOnlyItem: Boolean = false,
    val shoppingOnlyName: String? = null,
    val shoppingOnlyStore: String? = null,
    val shoppingOnlyPrice: Double? = null,
    val shoppingOnlyUnit: String? = null,
    val shoppingOnlyCategory: String? = null,
    val shoppingOnlyImageUri: String? = null,

    // Snapshots for completed carts
    val vaultItemNameSnapshot: String? = null,
    val vaultItemCategorySnapshot: String? = null,

    // Restoration / shopping additions
    val originalPlanningQuantity: Double? = null,
    val addedDuringShopping: Boolean = false,

    // Fulfillment animation state
    val fulfillmentAnimationState: Int = 0,
    val fulfillmentStartTime: Date? = null,
    val shouldShowCheckmark: Boolean = false,
    val shouldStrikethrough: Boolean = false,

    // Sale tracking
    val isOnSale: Boolean = false,
    val notes: String? = null,
    val saleType: String? = null,
    val discountValue: Double? = null,
    val regularPrice: Double? = null,
)
