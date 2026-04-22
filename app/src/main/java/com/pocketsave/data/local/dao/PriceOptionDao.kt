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

    /**
     * Batched replacement for looping [listForItem] over a list of item ids.
     * Callers group the result by `itemId` in memory (see [kotlin.collections.groupBy]).
     */
    @Query("SELECT * FROM price_options WHERE itemId IN (:itemIds)")
    suspend fun listForItems(itemIds: List<String>): List<PriceOptionEntity>

    @Query("SELECT * FROM price_options WHERE itemId = :itemId AND store = :store LIMIT 1")
    suspend fun findByItemAndStore(itemId: String, store: String): PriceOptionEntity?

    /**
     * Counts active vault items whose name matches (case-insensitive, trimmed) and
     * that have a price option at the given store. Replaces the per-item N+1
     * validation loop in `VaultService.isItemNameDuplicate`. The optional
     * `excludingItemId` supports the edit path.
     */
    @Query(
        "SELECT COUNT(*) FROM price_options p " +
            "INNER JOIN items i ON p.itemId = i.id " +
            "WHERE i.vaultUid = :vaultUid AND i.isDeleted = 0 " +
            "AND LOWER(TRIM(i.name)) = LOWER(TRIM(:name)) " +
            "AND LOWER(TRIM(p.store)) = LOWER(TRIM(:store)) " +
            "AND (:excludingItemId IS NULL OR i.id != :excludingItemId)",
    )
    suspend fun countDuplicateByNameAndStore(
        vaultUid: String,
        name: String,
        store: String,
        excludingItemId: String?,
    ): Int

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
