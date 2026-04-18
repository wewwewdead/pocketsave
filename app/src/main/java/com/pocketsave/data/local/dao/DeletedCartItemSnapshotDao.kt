package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketsave.data.local.entity.DeletedCartItemSnapshotEntity

@Dao
interface DeletedCartItemSnapshotDao {
    @Query("SELECT * FROM deleted_cart_item_snapshots WHERE cartId = :cartId")
    suspend fun listByCart(cartId: String): List<DeletedCartItemSnapshotEntity>

    @Query("SELECT * FROM deleted_cart_item_snapshots WHERE itemId = :itemId")
    suspend fun listByItem(itemId: String): List<DeletedCartItemSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: DeletedCartItemSnapshotEntity)

    @Delete
    suspend fun delete(snapshot: DeletedCartItemSnapshotEntity)
}
