package com.pocketsave.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors the iOS `PriceOption` type (per-store price for an item). */
@Entity(
    tableName = "price_options",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("itemId"),
        Index(value = ["itemId", "store"], unique = true),
    ],
)
data class PriceOptionEntity(
    @PrimaryKey val uid: String,
    val itemId: String,
    val store: String,
    @Embedded val pricePerUnit: PricePerUnit,
)
