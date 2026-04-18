package com.pocketsave.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketsave.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE vaultUid = :vaultUid ORDER BY sortOrder ASC")
    fun observeByVault(vaultUid: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE vaultUid = :vaultUid ORDER BY sortOrder ASC")
    suspend fun listByVault(vaultUid: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE uid = :uid")
    suspend fun findByUid(uid: String): CategoryEntity?

    /** Batched [findByUid] — returns in arbitrary order; callers associateBy { uid }. */
    @Query("SELECT * FROM categories WHERE uid IN (:uids)")
    suspend fun findByUids(uids: List<String>): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE vaultUid = :vaultUid AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByName(vaultUid: String, name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}
