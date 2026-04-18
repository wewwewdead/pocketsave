package com.pocketsave.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pocketsave.data.local.converter.Converters
import com.pocketsave.data.local.dao.CartDao
import com.pocketsave.data.local.dao.CartItemDao
import com.pocketsave.data.local.dao.CategoryDao
import com.pocketsave.data.local.dao.DeletedCartItemSnapshotDao
import com.pocketsave.data.local.dao.ItemDao
import com.pocketsave.data.local.dao.PriceOptionDao
import com.pocketsave.data.local.dao.StoreDao
import com.pocketsave.data.local.dao.UserDao
import com.pocketsave.data.local.dao.VaultDao
import com.pocketsave.data.local.entity.CartEntity
import com.pocketsave.data.local.entity.CartItemEntity
import com.pocketsave.data.local.entity.CategoryEntity
import com.pocketsave.data.local.entity.DeletedCartItemSnapshotEntity
import com.pocketsave.data.local.entity.ItemEntity
import com.pocketsave.data.local.entity.PriceOptionEntity
import com.pocketsave.data.local.entity.StoreEntity
import com.pocketsave.data.local.entity.UserEntity
import com.pocketsave.data.local.entity.VaultEntity

@Database(
    entities = [
        UserEntity::class,
        VaultEntity::class,
        StoreEntity::class,
        CategoryEntity::class,
        ItemEntity::class,
        PriceOptionEntity::class,
        CartEntity::class,
        CartItemEntity::class,
        DeletedCartItemSnapshotEntity::class,
    ],
    // v2: `categories.emoji` → `categories.iconKey` (AppIcon key).
    // Destructive migration is acceptable pre-launch via `fallbackToDestructiveMigration()`.
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PocketSaveDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vaultDao(): VaultDao
    abstract fun storeDao(): StoreDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun priceOptionDao(): PriceOptionDao
    abstract fun cartDao(): CartDao
    abstract fun cartItemDao(): CartItemDao
    abstract fun deletedCartItemSnapshotDao(): DeletedCartItemSnapshotDao

    companion object {
        const val DATABASE_NAME = "pocketsave.db"
    }
}
