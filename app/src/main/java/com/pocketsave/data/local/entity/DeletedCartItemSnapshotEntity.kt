package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors the iOS `DeletedCartItemSnapshot` type. */
@Entity(
    tableName = "deleted_cart_item_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("itemId"), Index("cartId")],
)
data class DeletedCartItemSnapshotEntity(
    @PrimaryKey val uid: String,
    val cartId: String,
    val itemId: String? = null,
    val quantity: Double,
    val plannedStore: String,
    val plannedPrice: Double? = null,
    val plannedUnit: String? = null,
    val actualStore: String? = null,
    val actualPrice: Double? = null,
    val actualQuantity: Double? = null,
    val actualUnit: String? = null,
    val wasEditedDuringShopping: Boolean = false,
    val wasFulfilled: Boolean = false,
)
