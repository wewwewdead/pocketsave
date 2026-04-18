package com.pocketsave.core.widget

import kotlinx.serialization.Serializable

/**
 * Port of iOS `PurchasedItemsWidgetSnapshot` from
 * `PocketSave/WidgetSupport/PocketSavePurchasedItemsWidgetSync.swift`.
 *
 * Serialised to JSON under app-private storage so the Glance widget host can
 * render without hitting Room or bootstrapping DI. Exact field layout matches
 * iOS so a future cross-platform share would be one-line to round-trip.
 */
@Serializable
data class PurchasedItemsWidgetSnapshot(
    val generatedAtMillis: Long,
    val cart: CartSnapshot? = null,
) {
    @Serializable
    data class CartSnapshot(
        val id: String,
        val name: String,
        val status: String,
        val purchasedCount: Int,
        val totalCount: Int,
        val totalSpending: Double,
        /**
         * Pre-formatted currency string. Widget providers run outside the app's
         * Compose tree so they can't read `LocalCurrencyFormatter`; the snapshot
         * writer resolves the user's override at write time and bakes it in.
         */
        val totalSpendingLabel: String = "",
        val items: List<ItemSnapshot>,
    )

    @Serializable
    data class ItemSnapshot(
        val id: String,
        val name: String,
        val imageUri: String? = null,
        val isFulfilled: Boolean,
    )

    companion object {
        const val PAGE_SIZE = 20

        /** Preview data, mirroring iOS `PurchasedItemsWidgetSnapshot.placeholder`. */
        val PLACEHOLDER = PurchasedItemsWidgetSnapshot(
            generatedAtMillis = 0L,
            cart = CartSnapshot(
                id = "preview-cart",
                name = "Sunday Groceries",
                status = "Shopping",
                purchasedCount = 12,
                totalCount = 20,
                totalSpending = 48.75,
                items = (1..20).map {
                    ItemSnapshot(
                        id = "preview-$it",
                        name = "Item $it",
                        imageUri = null,
                        isFulfilled = it <= 12,
                    )
                },
            ),
        )
    }
}
