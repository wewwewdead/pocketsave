package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketsave.data.local.entity.VaultEntity

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults WHERE uid = :uid")
    suspend fun findByUid(uid: String): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vault: VaultEntity)
}
