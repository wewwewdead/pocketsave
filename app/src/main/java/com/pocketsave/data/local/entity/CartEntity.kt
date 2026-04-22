package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Mirrors the iOS `Cart` type. `status` is stored as the raw int used by the Swift
 * enum (`CartStatus.raw`) so the schema is directly compatible.
 */
@Entity(
    tableName = "carts",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["uid"],
            childColumns = ["vaultUid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("vaultUid"),
        Index("status"),
        // Composite for the hot list queries:
        //   `vaultUid = ? AND isDeleted = 0 ORDER BY createdAt DESC`  (listActiveByVault)
        //   `vaultUid = ? AND isDeleted = 1 ORDER BY deletedAt DESC`  (listDeleted)
        // SQLite uses the leading columns; the ORDER BY is served by an in-memory
        // sort of the already-filtered set, which is tiny in practice.
        Index(value = ["vaultUid", "isDeleted"]),
    ],
)
data class CartEntity(
    @PrimaryKey val id: String,
    val vaultUid: String,
    val name: String,
    val budget: Double,
    val fulfillmentStatus: Double = 0.0,
    val createdAt: Date,
    val updatedAt: Date,
    val startedAt: Date? = null,
    val completedAt: Date? = null,
    val status: Int,
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
)
