package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketsave.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE vaultUid = :vaultUid AND isDeleted = 0")
    fun observeActive(vaultUid: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE vaultUid = :vaultUid AND isDeleted = 0")
    suspend fun listActive(vaultUid: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE vaultUid = :vaultUid AND isDeleted = 1")
    suspend fun listDeleted(vaultUid: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE categoryUid = :categoryUid AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun listByCategory(categoryUid: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun findById(id: String): ItemEntity?

    /** Batched [findById] — returns in arbitrary order; callers associateBy { id }. */
    @Query("SELECT * FROM items WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<String>): List<ItemEntity>

    @Query("SELECT * FROM items WHERE vaultUid = :vaultUid AND LOWER(name) = LOWER(:name) AND isDeleted = 0")
    suspend fun findByNameExact(vaultUid: String, name: String): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)
}
