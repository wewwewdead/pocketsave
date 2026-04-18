package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketsave.data.local.entity.PriceOptionEntity

@Dao
interface PriceOptionDao {
    @Query("SELECT * FROM price_options WHERE itemId = :itemId")
    suspend fun listForItem(itemId: String): List<PriceOptionEntity>

    @Query("SELECT * FROM price_options WHERE itemId = :itemId AND store = :store LIMIT 1")
    suspend fun findByItemAndStore(itemId: String, store: String): PriceOptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(priceOption: PriceOptionEntity)

    @Update
    suspend fun update(priceOption: PriceOptionEntity)

    @Delete
    suspend fun delete(priceOption: PriceOptionEntity)

    @Query("DELETE FROM price_options WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: String)

    @Query("DELETE FROM price_options WHERE store = :store AND itemId IN (SELECT id FROM items WHERE vaultUid = :vaultUid)")
    suspend fun deleteAllForStoreInVault(vaultUid: String, store: String)

    @Query(
        "SELECT p.* FROM price_options p " +
            "INNER JOIN items i ON p.itemId = i.id " +
            "WHERE i.vaultUid = :vaultUid AND i.isDeleted = 0",
    )
    suspend fun listActiveByVault(vaultUid: String): List<PriceOptionEntity>
}
