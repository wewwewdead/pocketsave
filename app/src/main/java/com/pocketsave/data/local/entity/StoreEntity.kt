package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Mirrors the iOS `Store` type. iOS identifies stores by `name` and `createdAt` only;
 * Room requires a stable primary key, so we add a UUID `uid`. A unique index on
 * (vaultUid, name) preserves the iOS invariant that store names are unique per vault.
 */
@Entity(
    tableName = "stores",
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
        Index(value = ["vaultUid", "name"], unique = true),
    ],
)
data class StoreEntity(
    @PrimaryKey val uid: String,
    val vaultUid: String,
    val name: String,
    val createdAt: Date,
)
