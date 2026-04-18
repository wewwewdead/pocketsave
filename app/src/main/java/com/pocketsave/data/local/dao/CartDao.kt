package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketsave.data.local.entity.CartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM carts WHERE vaultUid = :vaultUid AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeActive(vaultUid: String): Flow<List<CartEntity>>

    @Query("SELECT * FROM carts WHERE vaultUid = :vaultUid AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun listActiveByVault(vaultUid: String): List<CartEntity>

    @Query("SELECT * FROM carts WHERE vaultUid = :vaultUid AND isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun listDeleted(vaultUid: String): List<CartEntity>

    @Query("SELECT * FROM carts WHERE id = :id")
    suspend fun findById(id: String): CartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cart: CartEntity)

    @Update
    suspend fun update(cart: CartEntity)

    @Delete
    suspend fun delete(cart: CartEntity)
}
