package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketsave.data.local.entity.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores WHERE vaultUid = :vaultUid ORDER BY createdAt ASC")
    fun observeByVault(vaultUid: String): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE vaultUid = :vaultUid ORDER BY createdAt ASC")
    suspend fun listByVault(vaultUid: String): List<StoreEntity>

    @Query("SELECT * FROM stores WHERE vaultUid = :vaultUid ORDER BY createdAt DESC LIMIT 1")
    suspend fun mostRecent(vaultUid: String): StoreEntity?

    @Query("SELECT * FROM stores WHERE vaultUid = :vaultUid AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByName(vaultUid: String, name: String): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(store: StoreEntity): Long

    @Update
    suspend fun update(store: StoreEntity)

    @Delete
    suspend fun delete(store: StoreEntity)
}
