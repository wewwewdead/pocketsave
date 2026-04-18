package com.pocketsave.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.pocketsave.data.local.dao.CartDao;
import com.pocketsave.data.local.dao.CartDao_Impl;
import com.pocketsave.data.local.dao.CartItemDao;
import com.pocketsave.data.local.dao.CartItemDao_Impl;
import com.pocketsave.data.local.dao.CategoryDao;
import com.pocketsave.data.local.dao.CategoryDao_Impl;
import com.pocketsave.data.local.dao.DeletedCartItemSnapshotDao;
import com.pocketsave.data.local.dao.DeletedCartItemSnapshotDao_Impl;
import com.pocketsave.data.local.dao.ItemDao;
import com.pocketsave.data.local.dao.ItemDao_Impl;
import com.pocketsave.data.local.dao.PriceOptionDao;
import com.pocketsave.data.local.dao.PriceOptionDao_Impl;
import com.pocketsave.data.local.dao.StoreDao;
import com.pocketsave.data.local.dao.StoreDao_Impl;
import com.pocketsave.data.local.dao.UserDao;
import com.pocketsave.data.local.dao.UserDao_Impl;
import com.pocketsave.data.local.dao.VaultDao;
import com.pocketsave.data.local.dao.VaultDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PocketSaveDatabase_Impl extends PocketSaveDatabase {
  private volatile UserDao _userDao;

  private volatile VaultDao _vaultDao;

  private volatile StoreDao _storeDao;

  private volatile CategoryDao _categoryDao;

  private volatile ItemDao _itemDao;

  private volatile PriceOptionDao _priceOptionDao;

  private volatile CartDao _cartDao;

  private volatile CartItemDao _cartItemDao;

  private volatile DeletedCartItemSnapshotDao _deletedCartItemSnapshotDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `vaultUid` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`vaultUid`) REFERENCES `vaults`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_vaultUid` ON `users` (`vaultUid`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `vaults` (`uid` TEXT NOT NULL, PRIMARY KEY(`uid`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stores` (`uid` TEXT NOT NULL, `vaultUid` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`uid`), FOREIGN KEY(`vaultUid`) REFERENCES `vaults`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stores_vaultUid` ON `stores` (`vaultUid`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stores_vaultUid_name` ON `stores` (`vaultUid`, `name`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`uid` TEXT NOT NULL, `vaultUid` TEXT NOT NULL, `name` TEXT NOT NULL, `iconKey` TEXT, `sortOrder` INTEGER NOT NULL, `colorHex` TEXT, `isPlanSuppressed` INTEGER NOT NULL, `planSuppressedAt` INTEGER, `planSuppressedReason` TEXT, PRIMARY KEY(`uid`), FOREIGN KEY(`vaultUid`) REFERENCES `vaults`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_vaultUid` ON `categories` (`vaultUid`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `items` (`id` TEXT NOT NULL, `vaultUid` TEXT NOT NULL, `categoryUid` TEXT, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isTemporaryShoppingItem` INTEGER NOT NULL, `shoppingPrice` REAL, `shoppingUnit` TEXT, `isOnSale` INTEGER NOT NULL, `notes` TEXT, `saleType` TEXT, `discountValue` REAL, `regularPrice` REAL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `deletedFromCategoryName` TEXT, `isPlanSuppressed` INTEGER NOT NULL, `planSuppressedAt` INTEGER, `planSuppressedReason` TEXT, `imageUri` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`categoryUid`) REFERENCES `categories`(`uid`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`vaultUid`) REFERENCES `vaults`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_categoryUid` ON `items` (`categoryUid`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_vaultUid` ON `items` (`vaultUid`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_isDeleted` ON `items` (`isDeleted`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `price_options` (`uid` TEXT NOT NULL, `itemId` TEXT NOT NULL, `store` TEXT NOT NULL, `priceValue` REAL NOT NULL, `unit` TEXT NOT NULL, `packageSizeValue` REAL, `packageSizeUnit` TEXT, `outerPackagingUnit` TEXT, `outerPackagingConfidence` REAL, `outerPackagingSource` TEXT, PRIMARY KEY(`uid`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_options_itemId` ON `price_options` (`itemId`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_price_options_itemId_store` ON `price_options` (`itemId`, `store`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `carts` (`id` TEXT NOT NULL, `vaultUid` TEXT NOT NULL, `name` TEXT NOT NULL, `budget` REAL NOT NULL, `fulfillmentStatus` REAL NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `startedAt` INTEGER, `completedAt` INTEGER, `status` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`vaultUid`) REFERENCES `vaults`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_carts_vaultUid` ON `carts` (`vaultUid`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_carts_status` ON `carts` (`status`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_carts_isDeleted` ON `carts` (`isDeleted`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cart_items` (`uid` TEXT NOT NULL, `cartId` TEXT NOT NULL, `itemId` TEXT NOT NULL, `addedAt` INTEGER, `quantity` REAL NOT NULL, `isFulfilled` INTEGER NOT NULL, `isSkippedDuringShopping` INTEGER NOT NULL, `plannedStore` TEXT NOT NULL, `plannedPrice` REAL, `plannedUnit` TEXT, `actualStore` TEXT, `actualPrice` REAL, `actualQuantity` REAL, `actualUnit` TEXT, `wasEditedDuringShopping` INTEGER NOT NULL, `isShoppingOnlyItem` INTEGER NOT NULL, `shoppingOnlyName` TEXT, `shoppingOnlyStore` TEXT, `shoppingOnlyPrice` REAL, `shoppingOnlyUnit` TEXT, `shoppingOnlyCategory` TEXT, `shoppingOnlyImageUri` TEXT, `vaultItemNameSnapshot` TEXT, `vaultItemCategorySnapshot` TEXT, `originalPlanningQuantity` REAL, `addedDuringShopping` INTEGER NOT NULL, `fulfillmentAnimationState` INTEGER NOT NULL, `fulfillmentStartTime` INTEGER, `shouldShowCheckmark` INTEGER NOT NULL, `shouldStrikethrough` INTEGER NOT NULL, `isOnSale` INTEGER NOT NULL, `notes` TEXT, `saleType` TEXT, `discountValue` REAL, `regularPrice` REAL, PRIMARY KEY(`uid`), FOREIGN KEY(`cartId`) REFERENCES `carts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cart_items_cartId` ON `cart_items` (`cartId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cart_items_itemId` ON `cart_items` (`itemId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `deleted_cart_item_snapshots` (`uid` TEXT NOT NULL, `cartId` TEXT NOT NULL, `itemId` TEXT, `quantity` REAL NOT NULL, `plannedStore` TEXT NOT NULL, `plannedPrice` REAL, `plannedUnit` TEXT, `actualStore` TEXT, `actualPrice` REAL, `actualQuantity` REAL, `actualUnit` TEXT, `wasEditedDuringShopping` INTEGER NOT NULL, `wasFulfilled` INTEGER NOT NULL, PRIMARY KEY(`uid`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_cart_item_snapshots_itemId` ON `deleted_cart_item_snapshots` (`itemId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_cart_item_snapshots_cartId` ON `deleted_cart_item_snapshots` (`cartId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7ff88265609adb98310b107bc8eca986')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `vaults`");
        db.execSQL("DROP TABLE IF EXISTS `stores`");
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `items`");
        db.execSQL("DROP TABLE IF EXISTS `price_options`");
        db.execSQL("DROP TABLE IF EXISTS `carts`");
        db.execSQL("DROP TABLE IF EXISTS `cart_items`");
        db.execSQL("DROP TABLE IF EXISTS `deleted_cart_item_snapshots`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(3);
        _columnsUsers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("vaultUid", new TableInfo.Column("vaultUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysUsers.add(new TableInfo.ForeignKey("vaults", "CASCADE", "NO ACTION", Arrays.asList("vaultUid"), Arrays.asList("uid")));
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(1);
        _indicesUsers.add(new TableInfo.Index("index_users_vaultUid", false, Arrays.asList("vaultUid"), Arrays.asList("ASC")));
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.pocketsave.data.local.entity.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsVaults = new HashMap<String, TableInfo.Column>(1);
        _columnsVaults.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVaults = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVaults = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVaults = new TableInfo("vaults", _columnsVaults, _foreignKeysVaults, _indicesVaults);
        final TableInfo _existingVaults = TableInfo.read(db, "vaults");
        if (!_infoVaults.equals(_existingVaults)) {
          return new RoomOpenHelper.ValidationResult(false, "vaults(com.pocketsave.data.local.entity.VaultEntity).\n"
                  + " Expected:\n" + _infoVaults + "\n"
                  + " Found:\n" + _existingVaults);
        }
        final HashMap<String, TableInfo.Column> _columnsStores = new HashMap<String, TableInfo.Column>(4);
        _columnsStores.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("vaultUid", new TableInfo.Column("vaultUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStores = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysStores.add(new TableInfo.ForeignKey("vaults", "CASCADE", "NO ACTION", Arrays.asList("vaultUid"), Arrays.asList("uid")));
        final HashSet<TableInfo.Index> _indicesStores = new HashSet<TableInfo.Index>(2);
        _indicesStores.add(new TableInfo.Index("index_stores_vaultUid", false, Arrays.asList("vaultUid"), Arrays.asList("ASC")));
        _indicesStores.add(new TableInfo.Index("index_stores_vaultUid_name", true, Arrays.asList("vaultUid", "name"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoStores = new TableInfo("stores", _columnsStores, _foreignKeysStores, _indicesStores);
        final TableInfo _existingStores = TableInfo.read(db, "stores");
        if (!_infoStores.equals(_existingStores)) {
          return new RoomOpenHelper.ValidationResult(false, "stores(com.pocketsave.data.local.entity.StoreEntity).\n"
                  + " Expected:\n" + _infoStores + "\n"
                  + " Found:\n" + _existingStores);
        }
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(9);
        _columnsCategories.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("vaultUid", new TableInfo.Column("vaultUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("iconKey", new TableInfo.Column("iconKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("colorHex", new TableInfo.Column("colorHex", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("isPlanSuppressed", new TableInfo.Column("isPlanSuppressed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("planSuppressedAt", new TableInfo.Column("planSuppressedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("planSuppressedReason", new TableInfo.Column("planSuppressedReason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCategories.add(new TableInfo.ForeignKey("vaults", "CASCADE", "NO ACTION", Arrays.asList("vaultUid"), Arrays.asList("uid")));
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(1);
        _indicesCategories.add(new TableInfo.Index("index_categories_vaultUid", false, Arrays.asList("vaultUid"), Arrays.asList("ASC")));
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.pocketsave.data.local.entity.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsItems = new HashMap<String, TableInfo.Column>(20);
        _columnsItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("vaultUid", new TableInfo.Column("vaultUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("categoryUid", new TableInfo.Column("categoryUid", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("isTemporaryShoppingItem", new TableInfo.Column("isTemporaryShoppingItem", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("shoppingPrice", new TableInfo.Column("shoppingPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("shoppingUnit", new TableInfo.Column("shoppingUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("isOnSale", new TableInfo.Column("isOnSale", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("saleType", new TableInfo.Column("saleType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("discountValue", new TableInfo.Column("discountValue", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("regularPrice", new TableInfo.Column("regularPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("deletedFromCategoryName", new TableInfo.Column("deletedFromCategoryName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("isPlanSuppressed", new TableInfo.Column("isPlanSuppressed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("planSuppressedAt", new TableInfo.Column("planSuppressedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("planSuppressedReason", new TableInfo.Column("planSuppressedReason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsItems.put("imageUri", new TableInfo.Column("imageUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysItems = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysItems.add(new TableInfo.ForeignKey("categories", "SET NULL", "NO ACTION", Arrays.asList("categoryUid"), Arrays.asList("uid")));
        _foreignKeysItems.add(new TableInfo.ForeignKey("vaults", "CASCADE", "NO ACTION", Arrays.asList("vaultUid"), Arrays.asList("uid")));
        final HashSet<TableInfo.Index> _indicesItems = new HashSet<TableInfo.Index>(3);
        _indicesItems.add(new TableInfo.Index("index_items_categoryUid", false, Arrays.asList("categoryUid"), Arrays.asList("ASC")));
        _indicesItems.add(new TableInfo.Index("index_items_vaultUid", false, Arrays.asList("vaultUid"), Arrays.asList("ASC")));
        _indicesItems.add(new TableInfo.Index("index_items_isDeleted", false, Arrays.asList("isDeleted"), Arrays.asList("ASC")));
        final TableInfo _infoItems = new TableInfo("items", _columnsItems, _foreignKeysItems, _indicesItems);
        final TableInfo _existingItems = TableInfo.read(db, "items");
        if (!_infoItems.equals(_existingItems)) {
          return new RoomOpenHelper.ValidationResult(false, "items(com.pocketsave.data.local.entity.ItemEntity).\n"
                  + " Expected:\n" + _infoItems + "\n"
                  + " Found:\n" + _existingItems);
        }
        final HashMap<String, TableInfo.Column> _columnsPriceOptions = new HashMap<String, TableInfo.Column>(10);
        _columnsPriceOptions.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("itemId", new TableInfo.Column("itemId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("store", new TableInfo.Column("store", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("priceValue", new TableInfo.Column("priceValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("packageSizeValue", new TableInfo.Column("packageSizeValue", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("packageSizeUnit", new TableInfo.Column("packageSizeUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("outerPackagingUnit", new TableInfo.Column("outerPackagingUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("outerPackagingConfidence", new TableInfo.Column("outerPackagingConfidence", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceOptions.put("outerPackagingSource", new TableInfo.Column("outerPackagingSource", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPriceOptions = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysPriceOptions.add(new TableInfo.ForeignKey("items", "CASCADE", "NO ACTION", Arrays.asList("itemId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPriceOptions = new HashSet<TableInfo.Index>(2);
        _indicesPriceOptions.add(new TableInfo.Index("index_price_options_itemId", false, Arrays.asList("itemId"), Arrays.asList("ASC")));
        _indicesPriceOptions.add(new TableInfo.Index("index_price_options_itemId_store", true, Arrays.asList("itemId", "store"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoPriceOptions = new TableInfo("price_options", _columnsPriceOptions, _foreignKeysPriceOptions, _indicesPriceOptions);
        final TableInfo _existingPriceOptions = TableInfo.read(db, "price_options");
        if (!_infoPriceOptions.equals(_existingPriceOptions)) {
          return new RoomOpenHelper.ValidationResult(false, "price_options(com.pocketsave.data.local.entity.PriceOptionEntity).\n"
                  + " Expected:\n" + _infoPriceOptions + "\n"
                  + " Found:\n" + _existingPriceOptions);
        }
        final HashMap<String, TableInfo.Column> _columnsCarts = new HashMap<String, TableInfo.Column>(12);
        _columnsCarts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("vaultUid", new TableInfo.Column("vaultUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("budget", new TableInfo.Column("budget", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("fulfillmentStatus", new TableInfo.Column("fulfillmentStatus", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("startedAt", new TableInfo.Column("startedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("status", new TableInfo.Column("status", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCarts.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCarts = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCarts.add(new TableInfo.ForeignKey("vaults", "CASCADE", "NO ACTION", Arrays.asList("vaultUid"), Arrays.asList("uid")));
        final HashSet<TableInfo.Index> _indicesCarts = new HashSet<TableInfo.Index>(3);
        _indicesCarts.add(new TableInfo.Index("index_carts_vaultUid", false, Arrays.asList("vaultUid"), Arrays.asList("ASC")));
        _indicesCarts.add(new TableInfo.Index("index_carts_status", false, Arrays.asList("status"), Arrays.asList("ASC")));
        _indicesCarts.add(new TableInfo.Index("index_carts_isDeleted", false, Arrays.asList("isDeleted"), Arrays.asList("ASC")));
        final TableInfo _infoCarts = new TableInfo("carts", _columnsCarts, _foreignKeysCarts, _indicesCarts);
        final TableInfo _existingCarts = TableInfo.read(db, "carts");
        if (!_infoCarts.equals(_existingCarts)) {
          return new RoomOpenHelper.ValidationResult(false, "carts(com.pocketsave.data.local.entity.CartEntity).\n"
                  + " Expected:\n" + _infoCarts + "\n"
                  + " Found:\n" + _existingCarts);
        }
        final HashMap<String, TableInfo.Column> _columnsCartItems = new HashMap<String, TableInfo.Column>(35);
        _columnsCartItems.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("cartId", new TableInfo.Column("cartId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("itemId", new TableInfo.Column("itemId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("quantity", new TableInfo.Column("quantity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("isFulfilled", new TableInfo.Column("isFulfilled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("isSkippedDuringShopping", new TableInfo.Column("isSkippedDuringShopping", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("plannedStore", new TableInfo.Column("plannedStore", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("plannedPrice", new TableInfo.Column("plannedPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("plannedUnit", new TableInfo.Column("plannedUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("actualStore", new TableInfo.Column("actualStore", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("actualPrice", new TableInfo.Column("actualPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("actualQuantity", new TableInfo.Column("actualQuantity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("actualUnit", new TableInfo.Column("actualUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("wasEditedDuringShopping", new TableInfo.Column("wasEditedDuringShopping", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("isShoppingOnlyItem", new TableInfo.Column("isShoppingOnlyItem", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shoppingOnlyName", new TableInfo.Column("shoppingOnlyName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shoppingOnlyStore", new TableInfo.Column("shoppingOnlyStore", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shoppingOnlyPrice", new TableInfo.Column("shoppingOnlyPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shoppingOnlyUnit", new TableInfo.Column("shoppingOnlyUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shoppingOnlyCategory", new TableInfo.Column("shoppingOnlyCategory", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shoppingOnlyImageUri", new TableInfo.Column("shoppingOnlyImageUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("vaultItemNameSnapshot", new TableInfo.Column("vaultItemNameSnapshot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("vaultItemCategorySnapshot", new TableInfo.Column("vaultItemCategorySnapshot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("originalPlanningQuantity", new TableInfo.Column("originalPlanningQuantity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("addedDuringShopping", new TableInfo.Column("addedDuringShopping", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("fulfillmentAnimationState", new TableInfo.Column("fulfillmentAnimationState", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("fulfillmentStartTime", new TableInfo.Column("fulfillmentStartTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shouldShowCheckmark", new TableInfo.Column("shouldShowCheckmark", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("shouldStrikethrough", new TableInfo.Column("shouldStrikethrough", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("isOnSale", new TableInfo.Column("isOnSale", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("saleType", new TableInfo.Column("saleType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("discountValue", new TableInfo.Column("discountValue", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCartItems.put("regularPrice", new TableInfo.Column("regularPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCartItems = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCartItems.add(new TableInfo.ForeignKey("carts", "CASCADE", "NO ACTION", Arrays.asList("cartId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCartItems = new HashSet<TableInfo.Index>(2);
        _indicesCartItems.add(new TableInfo.Index("index_cart_items_cartId", false, Arrays.asList("cartId"), Arrays.asList("ASC")));
        _indicesCartItems.add(new TableInfo.Index("index_cart_items_itemId", false, Arrays.asList("itemId"), Arrays.asList("ASC")));
        final TableInfo _infoCartItems = new TableInfo("cart_items", _columnsCartItems, _foreignKeysCartItems, _indicesCartItems);
        final TableInfo _existingCartItems = TableInfo.read(db, "cart_items");
        if (!_infoCartItems.equals(_existingCartItems)) {
          return new RoomOpenHelper.ValidationResult(false, "cart_items(com.pocketsave.data.local.entity.CartItemEntity).\n"
                  + " Expected:\n" + _infoCartItems + "\n"
                  + " Found:\n" + _existingCartItems);
        }
        final HashMap<String, TableInfo.Column> _columnsDeletedCartItemSnapshots = new HashMap<String, TableInfo.Column>(13);
        _columnsDeletedCartItemSnapshots.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("cartId", new TableInfo.Column("cartId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("itemId", new TableInfo.Column("itemId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("quantity", new TableInfo.Column("quantity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("plannedStore", new TableInfo.Column("plannedStore", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("plannedPrice", new TableInfo.Column("plannedPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("plannedUnit", new TableInfo.Column("plannedUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("actualStore", new TableInfo.Column("actualStore", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("actualPrice", new TableInfo.Column("actualPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("actualQuantity", new TableInfo.Column("actualQuantity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("actualUnit", new TableInfo.Column("actualUnit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("wasEditedDuringShopping", new TableInfo.Column("wasEditedDuringShopping", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeletedCartItemSnapshots.put("wasFulfilled", new TableInfo.Column("wasFulfilled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDeletedCartItemSnapshots = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysDeletedCartItemSnapshots.add(new TableInfo.ForeignKey("items", "SET NULL", "NO ACTION", Arrays.asList("itemId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesDeletedCartItemSnapshots = new HashSet<TableInfo.Index>(2);
        _indicesDeletedCartItemSnapshots.add(new TableInfo.Index("index_deleted_cart_item_snapshots_itemId", false, Arrays.asList("itemId"), Arrays.asList("ASC")));
        _indicesDeletedCartItemSnapshots.add(new TableInfo.Index("index_deleted_cart_item_snapshots_cartId", false, Arrays.asList("cartId"), Arrays.asList("ASC")));
        final TableInfo _infoDeletedCartItemSnapshots = new TableInfo("deleted_cart_item_snapshots", _columnsDeletedCartItemSnapshots, _foreignKeysDeletedCartItemSnapshots, _indicesDeletedCartItemSnapshots);
        final TableInfo _existingDeletedCartItemSnapshots = TableInfo.read(db, "deleted_cart_item_snapshots");
        if (!_infoDeletedCartItemSnapshots.equals(_existingDeletedCartItemSnapshots)) {
          return new RoomOpenHelper.ValidationResult(false, "deleted_cart_item_snapshots(com.pocketsave.data.local.entity.DeletedCartItemSnapshotEntity).\n"
                  + " Expected:\n" + _infoDeletedCartItemSnapshots + "\n"
                  + " Found:\n" + _existingDeletedCartItemSnapshots);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "7ff88265609adb98310b107bc8eca986", "5b329ef1402b34aed37b1f724193d069");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "users","vaults","stores","categories","items","price_options","carts","cart_items","deleted_cart_item_snapshots");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `vaults`");
      _db.execSQL("DELETE FROM `stores`");
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `items`");
      _db.execSQL("DELETE FROM `price_options`");
      _db.execSQL("DELETE FROM `carts`");
      _db.execSQL("DELETE FROM `cart_items`");
      _db.execSQL("DELETE FROM `deleted_cart_item_snapshots`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(VaultDao.class, VaultDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StoreDao.class, StoreDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ItemDao.class, ItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PriceOptionDao.class, PriceOptionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CartDao.class, CartDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CartItemDao.class, CartItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DeletedCartItemSnapshotDao.class, DeletedCartItemSnapshotDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public VaultDao vaultDao() {
    if (_vaultDao != null) {
      return _vaultDao;
    } else {
      synchronized(this) {
        if(_vaultDao == null) {
          _vaultDao = new VaultDao_Impl(this);
        }
        return _vaultDao;
      }
    }
  }

  @Override
  public StoreDao storeDao() {
    if (_storeDao != null) {
      return _storeDao;
    } else {
      synchronized(this) {
        if(_storeDao == null) {
          _storeDao = new StoreDao_Impl(this);
        }
        return _storeDao;
      }
    }
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public ItemDao itemDao() {
    if (_itemDao != null) {
      return _itemDao;
    } else {
      synchronized(this) {
        if(_itemDao == null) {
          _itemDao = new ItemDao_Impl(this);
        }
        return _itemDao;
      }
    }
  }

  @Override
  public PriceOptionDao priceOptionDao() {
    if (_priceOptionDao != null) {
      return _priceOptionDao;
    } else {
      synchronized(this) {
        if(_priceOptionDao == null) {
          _priceOptionDao = new PriceOptionDao_Impl(this);
        }
        return _priceOptionDao;
      }
    }
  }

  @Override
  public CartDao cartDao() {
    if (_cartDao != null) {
      return _cartDao;
    } else {
      synchronized(this) {
        if(_cartDao == null) {
          _cartDao = new CartDao_Impl(this);
        }
        return _cartDao;
      }
    }
  }

  @Override
  public CartItemDao cartItemDao() {
    if (_cartItemDao != null) {
      return _cartItemDao;
    } else {
      synchronized(this) {
        if(_cartItemDao == null) {
          _cartItemDao = new CartItemDao_Impl(this);
        }
        return _cartItemDao;
      }
    }
  }

  @Override
  public DeletedCartItemSnapshotDao deletedCartItemSnapshotDao() {
    if (_deletedCartItemSnapshotDao != null) {
      return _deletedCartItemSnapshotDao;
    } else {
      synchronized(this) {
        if(_deletedCartItemSnapshotDao == null) {
          _deletedCartItemSnapshotDao = new DeletedCartItemSnapshotDao_Impl(this);
        }
        return _deletedCartItemSnapshotDao;
      }
    }
  }
}
