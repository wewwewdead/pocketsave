package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Mirrors the iOS `Vault` type; all other tables reference this `uid`. */
@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey val uid: String,
)
