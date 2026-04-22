package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketsave.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartItemDao {
    @Query("SELECT * FROM cart_items WHERE cartId = :cartId ORDER BY addedAt ASC")
    fun observeByCart(cartId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE cartId = :cartId ORDER BY addedAt ASC")
    suspend fun listByCart(cartId: String): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE cartId IN (:cartIds) ORDER BY cartId, addedAt ASC")
    suspend fun listForCarts(cartIds: List<String>): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE cartId = :cartId AND itemId = :itemId LIMIT 1")
    suspend fun findByCartAndItem(cartId: String, itemId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cartItem: CartItemEntity)

    @Update
    suspend fun update(cartItem: CartItemEntity)

    @Delete
    suspend fun delete(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE cartId = :cartId AND itemId = :itemId")
    suspend fun deleteByCartAndItem(cartId: String, itemId: String)
}
