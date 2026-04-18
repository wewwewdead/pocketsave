package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Mirrors the iOS `Category` type.
 *
 * [iconKey] replaces the previous `emoji` column as of DB v2. It maps into
 * the [com.pocketsave.core.vault.icons.AppIcon] registry; unknown keys fall
 * back to a generic label icon so old/new stored values stay renderable.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["uid"],
            childColumns = ["vaultUid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vaultUid")],
)
data class CategoryEntity(
    @PrimaryKey val uid: String,
    val vaultUid: String,
    val name: String,
    val iconKey: String? = null,
    val sortOrder: Int = 0,
    val colorHex: String? = null,
    val isPlanSuppressed: Boolean = false,
    val planSuppressedAt: Date? = null,
    val planSuppressedReason: String? = null,
)
