package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Mirrors the iOS `Item` type. The image is kept off-table: instead of a `Data` blob
 * (`@Attribute(.externalStorage)` on iOS), we persist a file path / content URI into
 * [imageUri] and store the bytes under the app's private storage.
 *
 * `vaultUid` is populated only when this item is soft-deleted and moved off its
 * category (iOS: `vault.deletedItems`). A live item keeps the link to its category
 * via [categoryUid] and can resolve its vault through the category row.
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["uid"],
            childColumns = ["categoryUid"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["uid"],
            childColumns = ["vaultUid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("categoryUid"),
        Index("vaultUid"),
        Index("isDeleted"),
    ],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val vaultUid: String,
    val categoryUid: String?,
    val name: String,
    val createdAt: Date,

    // Shopping context
    val isTemporaryShoppingItem: Boolean = false,
    val shoppingPrice: Double? = null,
    val shoppingUnit: String? = null,

    // Sale / future proofing
    val isOnSale: Boolean = false,
    val notes: String? = null,
    val saleType: String? = null,
    val discountValue: Double? = null,
    val regularPrice: Double? = null,

    // Soft delete
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
    val deletedFromCategoryName: String? = null,

    // Plan suppression
    val isPlanSuppressed: Boolean = false,
    val planSuppressedAt: Date? = null,
    val planSuppressedReason: String? = null,

    // Image stored off-row, path persisted here
    val imageUri: String? = null,
)
