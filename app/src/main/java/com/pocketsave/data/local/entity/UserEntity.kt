package com.pocketsave.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mirrors the iOS `User` type (`PocketSave/Models/Vault.swift`).
 *
 * PocketSave is single-user; exactly one row is expected to exist.
 * `vaultUid` is the foreign key to the owning [VaultEntity].
 */
@Entity(
    tableName = "users",
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
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val vaultUid: String,
)
