package com.pocketsave.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pocketsave.data.local.converter.Converters;
import com.pocketsave.data.local.entity.CartItemEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CartItemDao_Impl implements CartItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CartItemEntity> __insertionAdapterOfCartItemEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<CartItemEntity> __deletionAdapterOfCartItemEntity;

  private final EntityDeletionOrUpdateAdapter<CartItemEntity> __updateAdapterOfCartItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByCartAndItem;

  public CartItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCartItemEntity = new EntityInsertionAdapter<CartItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cart_items` (`uid`,`cartId`,`itemId`,`addedAt`,`quantity`,`isFulfilled`,`isSkippedDuringShopping`,`plannedStore`,`plannedPrice`,`plannedUnit`,`actualStore`,`actualPrice`,`actualQuantity`,`actualUnit`,`wasEditedDuringShopping`,`isShoppingOnlyItem`,`shoppingOnlyName`,`shoppingOnlyStore`,`shoppingOnlyPrice`,`shoppingOnlyUnit`,`shoppingOnlyCategory`,`shoppingOnlyImageUri`,`vaultItemNameSnapshot`,`vaultItemCategorySnapshot`,`originalPlanningQuantity`,`addedDuringShopping`,`fulfillmentAnimationState`,`fulfillmentStartTime`,`shouldShowCheckmark`,`shouldStrikethrough`,`isOnSale`,`notes`,`saleType`,`discountValue`,`regularPrice`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CartItemEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getCartId());
        statement.bindString(3, entity.getItemId());
        final Long _tmp = __converters.dateToLong(entity.getAddedAt());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp);
        }
        statement.bindDouble(5, entity.getQuantity());
        final int _tmp_1 = entity.isFulfilled() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        final int _tmp_2 = entity.isSkippedDuringShopping() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        statement.bindString(8, entity.getPlannedStore());
        if (entity.getPlannedPrice() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getPlannedPrice());
        }
        if (entity.getPlannedUnit() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPlannedUnit());
        }
        if (entity.getActualStore() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getActualStore());
        }
        if (entity.getActualPrice() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getActualPrice());
        }
        if (entity.getActualQuantity() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getActualQuantity());
        }
        if (entity.getActualUnit() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getActualUnit());
        }
        final int _tmp_3 = entity.getWasEditedDuringShopping() ? 1 : 0;
        statement.bindLong(15, _tmp_3);
        final int _tmp_4 = entity.isShoppingOnlyItem() ? 1 : 0;
        statement.bindLong(16, _tmp_4);
        if (entity.getShoppingOnlyName() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getShoppingOnlyName());
        }
        if (entity.getShoppingOnlyStore() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getShoppingOnlyStore());
        }
        if (entity.getShoppingOnlyPrice() == null) {
          statement.bindNull(19);
        } else {
          statement.bindDouble(19, entity.getShoppingOnlyPrice());
        }
        if (entity.getShoppingOnlyUnit() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getShoppingOnlyUnit());
        }
        if (entity.getShoppingOnlyCategory() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getShoppingOnlyCategory());
        }
        if (entity.getShoppingOnlyImageUri() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getShoppingOnlyImageUri());
        }
        if (entity.getVaultItemNameSnapshot() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getVaultItemNameSnapshot());
        }
        if (entity.getVaultItemCategorySnapshot() == null) {
          statement.bindNull(24);
        } else {
          statement.bindString(24, entity.getVaultItemCategorySnapshot());
        }
        if (entity.getOriginalPlanningQuantity() == null) {
          statement.bindNull(25);
        } else {
          statement.bindDouble(25, entity.getOriginalPlanningQuantity());
        }
        final int _tmp_5 = entity.getAddedDuringShopping() ? 1 : 0;
        statement.bindLong(26, _tmp_5);
        statement.bindLong(27, entity.getFulfillmentAnimationState());
        final Long _tmp_6 = __converters.dateToLong(entity.getFulfillmentStartTime());
        if (_tmp_6 == null) {
          statement.bindNull(28);
        } else {
          statement.bindLong(28, _tmp_6);
        }
        final int _tmp_7 = entity.getShouldShowCheckmark() ? 1 : 0;
        statement.bindLong(29, _tmp_7);
        final int _tmp_8 = entity.getShouldStrikethrough() ? 1 : 0;
        statement.bindLong(30, _tmp_8);
        final int _tmp_9 = entity.isOnSale() ? 1 : 0;
        statement.bindLong(31, _tmp_9);
        if (entity.getNotes() == null) {
          statement.bindNull(32);
        } else {
          statement.bindString(32, entity.getNotes());
        }
        if (entity.getSaleType() == null) {
          statement.bindNull(33);
        } else {
          statement.bindString(33, entity.getSaleType());
        }
        if (entity.getDiscountValue() == null) {
          statement.bindNull(34);
        } else {
          statement.bindDouble(34, entity.getDiscountValue());
        }
        if (entity.getRegularPrice() == null) {
          statement.bindNull(35);
        } else {
          statement.bindDouble(35, entity.getRegularPrice());
        }
      }
    };
    this.__deletionAdapterOfCartItemEntity = new EntityDeletionOrUpdateAdapter<CartItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `cart_items` WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CartItemEntity entity) {
        statement.bindString(1, entity.getUid());
      }
    };
    this.__updateAdapterOfCartItemEntity = new EntityDeletionOrUpdateAdapter<CartItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `cart_items` SET `uid` = ?,`cartId` = ?,`itemId` = ?,`addedAt` = ?,`quantity` = ?,`isFulfilled` = ?,`isSkippedDuringShopping` = ?,`plannedStore` = ?,`plannedPrice` = ?,`plannedUnit` = ?,`actualStore` = ?,`actualPrice` = ?,`actualQuantity` = ?,`actualUnit` = ?,`wasEditedDuringShopping` = ?,`isShoppingOnlyItem` = ?,`shoppingOnlyName` = ?,`shoppingOnlyStore` = ?,`shoppingOnlyPrice` = ?,`shoppingOnlyUnit` = ?,`shoppingOnlyCategory` = ?,`shoppingOnlyImageUri` = ?,`vaultItemNameSnapshot` = ?,`vaultItemCategorySnapshot` = ?,`originalPlanningQuantity` = ?,`addedDuringShopping` = ?,`fulfillmentAnimationState` = ?,`fulfillmentStartTime` = ?,`shouldShowCheckmark` = ?,`shouldStrikethrough` = ?,`isOnSale` = ?,`notes` = ?,`saleType` = ?,`discountValue` = ?,`regularPrice` = ? WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CartItemEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getCartId());
        statement.bindString(3, entity.getItemId());
        final Long _tmp = __converters.dateToLong(entity.getAddedAt());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp);
        }
        statement.bindDouble(5, entity.getQuantity());
        final int _tmp_1 = entity.isFulfilled() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        final int _tmp_2 = entity.isSkippedDuringShopping() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        statement.bindString(8, entity.getPlannedStore());
        if (entity.getPlannedPrice() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getPlannedPrice());
        }
        if (entity.getPlannedUnit() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPlannedUnit());
        }
        if (entity.getActualStore() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getActualStore());
        }
        if (entity.getActualPrice() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getActualPrice());
        }
        if (entity.getActualQuantity() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getActualQuantity());
        }
        if (entity.getActualUnit() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getActualUnit());
        }
        final int _tmp_3 = entity.getWasEditedDuringShopping() ? 1 : 0;
        statement.bindLong(15, _tmp_3);
        final int _tmp_4 = entity.isShoppingOnlyItem() ? 1 : 0;
        statement.bindLong(16, _tmp_4);
        if (entity.getShoppingOnlyName() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getShoppingOnlyName());
        }
        if (entity.getShoppingOnlyStore() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getShoppingOnlyStore());
        }
        if (entity.getShoppingOnlyPrice() == null) {
          statement.bindNull(19);
        } else {
          statement.bindDouble(19, entity.getShoppingOnlyPrice());
        }
        if (entity.getShoppingOnlyUnit() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getShoppingOnlyUnit());
        }
        if (entity.getShoppingOnlyCategory() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getShoppingOnlyCategory());
        }
        if (entity.getShoppingOnlyImageUri() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getShoppingOnlyImageUri());
        }
        if (entity.getVaultItemNameSnapshot() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getVaultItemNameSnapshot());
        }
        if (entity.getVaultItemCategorySnapshot() == null) {
          statement.bindNull(24);
        } else {
          statement.bindString(24, entity.getVaultItemCategorySnapshot());
        }
        if (entity.getOriginalPlanningQuantity() == null) {
          statement.bindNull(25);
        } else {
          statement.bindDouble(25, entity.getOriginalPlanningQuantity());
        }
        final int _tmp_5 = entity.getAddedDuringShopping() ? 1 : 0;
        statement.bindLong(26, _tmp_5);
        statement.bindLong(27, entity.getFulfillmentAnimationState());
        final Long _tmp_6 = __converters.dateToLong(entity.getFulfillmentStartTime());
        if (_tmp_6 == null) {
          statement.bindNull(28);
        } else {
          statement.bindLong(28, _tmp_6);
        }
        final int _tmp_7 = entity.getShouldShowCheckmark() ? 1 : 0;
        statement.bindLong(29, _tmp_7);
        final int _tmp_8 = entity.getShouldStrikethrough() ? 1 : 0;
        statement.bindLong(30, _tmp_8);
        final int _tmp_9 = entity.isOnSale() ? 1 : 0;
        statement.bindLong(31, _tmp_9);
        if (entity.getNotes() == null) {
          statement.bindNull(32);
        } else {
          statement.bindString(32, entity.getNotes());
        }
        if (entity.getSaleType() == null) {
          statement.bindNull(33);
        } else {
          statement.bindString(33, entity.getSaleType());
        }
        if (entity.getDiscountValue() == null) {
          statement.bindNull(34);
        } else {
          statement.bindDouble(34, entity.getDiscountValue());
        }
        if (entity.getRegularPrice() == null) {
          statement.bindNull(35);
        } else {
          statement.bindDouble(35, entity.getRegularPrice());
        }
        statement.bindString(36, entity.getUid());
      }
    };
    this.__preparedStmtOfDeleteByCartAndItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cart_items WHERE cartId = ? AND itemId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final CartItemEntity cartItem,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCartItemEntity.insert(cartItem);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final CartItemEntity cartItem,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCartItemEntity.handle(cartItem);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final CartItemEntity cartItem,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCartItemEntity.handle(cartItem);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByCartAndItem(final String cartId, final String itemId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByCartAndItem.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, cartId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, itemId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByCartAndItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CartItemEntity>> observeByCart(final String cartId) {
    final String _sql = "SELECT * FROM cart_items WHERE cartId = ? ORDER BY addedAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cartId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cart_items"}, new Callable<List<CartItemEntity>>() {
      @Override
      @NonNull
      public List<CartItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfCartId = CursorUtil.getColumnIndexOrThrow(_cursor, "cartId");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfIsFulfilled = CursorUtil.getColumnIndexOrThrow(_cursor, "isFulfilled");
          final int _cursorIndexOfIsSkippedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "isSkippedDuringShopping");
          final int _cursorIndexOfPlannedStore = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedStore");
          final int _cursorIndexOfPlannedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedPrice");
          final int _cursorIndexOfPlannedUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedUnit");
          final int _cursorIndexOfActualStore = CursorUtil.getColumnIndexOrThrow(_cursor, "actualStore");
          final int _cursorIndexOfActualPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "actualPrice");
          final int _cursorIndexOfActualQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "actualQuantity");
          final int _cursorIndexOfActualUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "actualUnit");
          final int _cursorIndexOfWasEditedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "wasEditedDuringShopping");
          final int _cursorIndexOfIsShoppingOnlyItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isShoppingOnlyItem");
          final int _cursorIndexOfShoppingOnlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyName");
          final int _cursorIndexOfShoppingOnlyStore = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyStore");
          final int _cursorIndexOfShoppingOnlyPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyPrice");
          final int _cursorIndexOfShoppingOnlyUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyUnit");
          final int _cursorIndexOfShoppingOnlyCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyCategory");
          final int _cursorIndexOfShoppingOnlyImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyImageUri");
          final int _cursorIndexOfVaultItemNameSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemNameSnapshot");
          final int _cursorIndexOfVaultItemCategorySnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemCategorySnapshot");
          final int _cursorIndexOfOriginalPlanningQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPlanningQuantity");
          final int _cursorIndexOfAddedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "addedDuringShopping");
          final int _cursorIndexOfFulfillmentAnimationState = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentAnimationState");
          final int _cursorIndexOfFulfillmentStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStartTime");
          final int _cursorIndexOfShouldShowCheckmark = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldShowCheckmark");
          final int _cursorIndexOfShouldStrikethrough = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldStrikethrough");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final List<CartItemEntity> _result = new ArrayList<CartItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CartItemEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpCartId;
            _tmpCartId = _cursor.getString(_cursorIndexOfCartId);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final Date _tmpAddedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _tmpAddedAt = __converters.longToDate(_tmp);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final boolean _tmpIsFulfilled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFulfilled);
            _tmpIsFulfilled = _tmp_1 != 0;
            final boolean _tmpIsSkippedDuringShopping;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsSkippedDuringShopping);
            _tmpIsSkippedDuringShopping = _tmp_2 != 0;
            final String _tmpPlannedStore;
            _tmpPlannedStore = _cursor.getString(_cursorIndexOfPlannedStore);
            final Double _tmpPlannedPrice;
            if (_cursor.isNull(_cursorIndexOfPlannedPrice)) {
              _tmpPlannedPrice = null;
            } else {
              _tmpPlannedPrice = _cursor.getDouble(_cursorIndexOfPlannedPrice);
            }
            final String _tmpPlannedUnit;
            if (_cursor.isNull(_cursorIndexOfPlannedUnit)) {
              _tmpPlannedUnit = null;
            } else {
              _tmpPlannedUnit = _cursor.getString(_cursorIndexOfPlannedUnit);
            }
            final String _tmpActualStore;
            if (_cursor.isNull(_cursorIndexOfActualStore)) {
              _tmpActualStore = null;
            } else {
              _tmpActualStore = _cursor.getString(_cursorIndexOfActualStore);
            }
            final Double _tmpActualPrice;
            if (_cursor.isNull(_cursorIndexOfActualPrice)) {
              _tmpActualPrice = null;
            } else {
              _tmpActualPrice = _cursor.getDouble(_cursorIndexOfActualPrice);
            }
            final Double _tmpActualQuantity;
            if (_cursor.isNull(_cursorIndexOfActualQuantity)) {
              _tmpActualQuantity = null;
            } else {
              _tmpActualQuantity = _cursor.getDouble(_cursorIndexOfActualQuantity);
            }
            final String _tmpActualUnit;
            if (_cursor.isNull(_cursorIndexOfActualUnit)) {
              _tmpActualUnit = null;
            } else {
              _tmpActualUnit = _cursor.getString(_cursorIndexOfActualUnit);
            }
            final boolean _tmpWasEditedDuringShopping;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWasEditedDuringShopping);
            _tmpWasEditedDuringShopping = _tmp_3 != 0;
            final boolean _tmpIsShoppingOnlyItem;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsShoppingOnlyItem);
            _tmpIsShoppingOnlyItem = _tmp_4 != 0;
            final String _tmpShoppingOnlyName;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyName)) {
              _tmpShoppingOnlyName = null;
            } else {
              _tmpShoppingOnlyName = _cursor.getString(_cursorIndexOfShoppingOnlyName);
            }
            final String _tmpShoppingOnlyStore;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyStore)) {
              _tmpShoppingOnlyStore = null;
            } else {
              _tmpShoppingOnlyStore = _cursor.getString(_cursorIndexOfShoppingOnlyStore);
            }
            final Double _tmpShoppingOnlyPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyPrice)) {
              _tmpShoppingOnlyPrice = null;
            } else {
              _tmpShoppingOnlyPrice = _cursor.getDouble(_cursorIndexOfShoppingOnlyPrice);
            }
            final String _tmpShoppingOnlyUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyUnit)) {
              _tmpShoppingOnlyUnit = null;
            } else {
              _tmpShoppingOnlyUnit = _cursor.getString(_cursorIndexOfShoppingOnlyUnit);
            }
            final String _tmpShoppingOnlyCategory;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyCategory)) {
              _tmpShoppingOnlyCategory = null;
            } else {
              _tmpShoppingOnlyCategory = _cursor.getString(_cursorIndexOfShoppingOnlyCategory);
            }
            final String _tmpShoppingOnlyImageUri;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyImageUri)) {
              _tmpShoppingOnlyImageUri = null;
            } else {
              _tmpShoppingOnlyImageUri = _cursor.getString(_cursorIndexOfShoppingOnlyImageUri);
            }
            final String _tmpVaultItemNameSnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemNameSnapshot)) {
              _tmpVaultItemNameSnapshot = null;
            } else {
              _tmpVaultItemNameSnapshot = _cursor.getString(_cursorIndexOfVaultItemNameSnapshot);
            }
            final String _tmpVaultItemCategorySnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemCategorySnapshot)) {
              _tmpVaultItemCategorySnapshot = null;
            } else {
              _tmpVaultItemCategorySnapshot = _cursor.getString(_cursorIndexOfVaultItemCategorySnapshot);
            }
            final Double _tmpOriginalPlanningQuantity;
            if (_cursor.isNull(_cursorIndexOfOriginalPlanningQuantity)) {
              _tmpOriginalPlanningQuantity = null;
            } else {
              _tmpOriginalPlanningQuantity = _cursor.getDouble(_cursorIndexOfOriginalPlanningQuantity);
            }
            final boolean _tmpAddedDuringShopping;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfAddedDuringShopping);
            _tmpAddedDuringShopping = _tmp_5 != 0;
            final int _tmpFulfillmentAnimationState;
            _tmpFulfillmentAnimationState = _cursor.getInt(_cursorIndexOfFulfillmentAnimationState);
            final Date _tmpFulfillmentStartTime;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfFulfillmentStartTime)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfFulfillmentStartTime);
            }
            _tmpFulfillmentStartTime = __converters.longToDate(_tmp_6);
            final boolean _tmpShouldShowCheckmark;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfShouldShowCheckmark);
            _tmpShouldShowCheckmark = _tmp_7 != 0;
            final boolean _tmpShouldStrikethrough;
            final int _tmp_8;
            _tmp_8 = _cursor.getInt(_cursorIndexOfShouldStrikethrough);
            _tmpShouldStrikethrough = _tmp_8 != 0;
            final boolean _tmpIsOnSale;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_9 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpSaleType;
            if (_cursor.isNull(_cursorIndexOfSaleType)) {
              _tmpSaleType = null;
            } else {
              _tmpSaleType = _cursor.getString(_cursorIndexOfSaleType);
            }
            final Double _tmpDiscountValue;
            if (_cursor.isNull(_cursorIndexOfDiscountValue)) {
              _tmpDiscountValue = null;
            } else {
              _tmpDiscountValue = _cursor.getDouble(_cursorIndexOfDiscountValue);
            }
            final Double _tmpRegularPrice;
            if (_cursor.isNull(_cursorIndexOfRegularPrice)) {
              _tmpRegularPrice = null;
            } else {
              _tmpRegularPrice = _cursor.getDouble(_cursorIndexOfRegularPrice);
            }
            _item = new CartItemEntity(_tmpUid,_tmpCartId,_tmpItemId,_tmpAddedAt,_tmpQuantity,_tmpIsFulfilled,_tmpIsSkippedDuringShopping,_tmpPlannedStore,_tmpPlannedPrice,_tmpPlannedUnit,_tmpActualStore,_tmpActualPrice,_tmpActualQuantity,_tmpActualUnit,_tmpWasEditedDuringShopping,_tmpIsShoppingOnlyItem,_tmpShoppingOnlyName,_tmpShoppingOnlyStore,_tmpShoppingOnlyPrice,_tmpShoppingOnlyUnit,_tmpShoppingOnlyCategory,_tmpShoppingOnlyImageUri,_tmpVaultItemNameSnapshot,_tmpVaultItemCategorySnapshot,_tmpOriginalPlanningQuantity,_tmpAddedDuringShopping,_tmpFulfillmentAnimationState,_tmpFulfillmentStartTime,_tmpShouldShowCheckmark,_tmpShouldStrikethrough,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object listByCart(final String cartId,
      final Continuation<? super List<CartItemEntity>> $completion) {
    final String _sql = "SELECT * FROM cart_items WHERE cartId = ? ORDER BY addedAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cartId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CartItemEntity>>() {
      @Override
      @NonNull
      public List<CartItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfCartId = CursorUtil.getColumnIndexOrThrow(_cursor, "cartId");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfIsFulfilled = CursorUtil.getColumnIndexOrThrow(_cursor, "isFulfilled");
          final int _cursorIndexOfIsSkippedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "isSkippedDuringShopping");
          final int _cursorIndexOfPlannedStore = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedStore");
          final int _cursorIndexOfPlannedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedPrice");
          final int _cursorIndexOfPlannedUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedUnit");
          final int _cursorIndexOfActualStore = CursorUtil.getColumnIndexOrThrow(_cursor, "actualStore");
          final int _cursorIndexOfActualPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "actualPrice");
          final int _cursorIndexOfActualQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "actualQuantity");
          final int _cursorIndexOfActualUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "actualUnit");
          final int _cursorIndexOfWasEditedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "wasEditedDuringShopping");
          final int _cursorIndexOfIsShoppingOnlyItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isShoppingOnlyItem");
          final int _cursorIndexOfShoppingOnlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyName");
          final int _cursorIndexOfShoppingOnlyStore = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyStore");
          final int _cursorIndexOfShoppingOnlyPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyPrice");
          final int _cursorIndexOfShoppingOnlyUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyUnit");
          final int _cursorIndexOfShoppingOnlyCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyCategory");
          final int _cursorIndexOfShoppingOnlyImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyImageUri");
          final int _cursorIndexOfVaultItemNameSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemNameSnapshot");
          final int _cursorIndexOfVaultItemCategorySnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemCategorySnapshot");
          final int _cursorIndexOfOriginalPlanningQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPlanningQuantity");
          final int _cursorIndexOfAddedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "addedDuringShopping");
          final int _cursorIndexOfFulfillmentAnimationState = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentAnimationState");
          final int _cursorIndexOfFulfillmentStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStartTime");
          final int _cursorIndexOfShouldShowCheckmark = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldShowCheckmark");
          final int _cursorIndexOfShouldStrikethrough = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldStrikethrough");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final List<CartItemEntity> _result = new ArrayList<CartItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CartItemEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpCartId;
            _tmpCartId = _cursor.getString(_cursorIndexOfCartId);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final Date _tmpAddedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _tmpAddedAt = __converters.longToDate(_tmp);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final boolean _tmpIsFulfilled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFulfilled);
            _tmpIsFulfilled = _tmp_1 != 0;
            final boolean _tmpIsSkippedDuringShopping;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsSkippedDuringShopping);
            _tmpIsSkippedDuringShopping = _tmp_2 != 0;
            final String _tmpPlannedStore;
            _tmpPlannedStore = _cursor.getString(_cursorIndexOfPlannedStore);
            final Double _tmpPlannedPrice;
            if (_cursor.isNull(_cursorIndexOfPlannedPrice)) {
              _tmpPlannedPrice = null;
            } else {
              _tmpPlannedPrice = _cursor.getDouble(_cursorIndexOfPlannedPrice);
            }
            final String _tmpPlannedUnit;
            if (_cursor.isNull(_cursorIndexOfPlannedUnit)) {
              _tmpPlannedUnit = null;
            } else {
              _tmpPlannedUnit = _cursor.getString(_cursorIndexOfPlannedUnit);
            }
            final String _tmpActualStore;
            if (_cursor.isNull(_cursorIndexOfActualStore)) {
              _tmpActualStore = null;
            } else {
              _tmpActualStore = _cursor.getString(_cursorIndexOfActualStore);
            }
            final Double _tmpActualPrice;
            if (_cursor.isNull(_cursorIndexOfActualPrice)) {
              _tmpActualPrice = null;
            } else {
              _tmpActualPrice = _cursor.getDouble(_cursorIndexOfActualPrice);
            }
            final Double _tmpActualQuantity;
            if (_cursor.isNull(_cursorIndexOfActualQuantity)) {
              _tmpActualQuantity = null;
            } else {
              _tmpActualQuantity = _cursor.getDouble(_cursorIndexOfActualQuantity);
            }
            final String _tmpActualUnit;
            if (_cursor.isNull(_cursorIndexOfActualUnit)) {
              _tmpActualUnit = null;
            } else {
              _tmpActualUnit = _cursor.getString(_cursorIndexOfActualUnit);
            }
            final boolean _tmpWasEditedDuringShopping;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWasEditedDuringShopping);
            _tmpWasEditedDuringShopping = _tmp_3 != 0;
            final boolean _tmpIsShoppingOnlyItem;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsShoppingOnlyItem);
            _tmpIsShoppingOnlyItem = _tmp_4 != 0;
            final String _tmpShoppingOnlyName;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyName)) {
              _tmpShoppingOnlyName = null;
            } else {
              _tmpShoppingOnlyName = _cursor.getString(_cursorIndexOfShoppingOnlyName);
            }
            final String _tmpShoppingOnlyStore;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyStore)) {
              _tmpShoppingOnlyStore = null;
            } else {
              _tmpShoppingOnlyStore = _cursor.getString(_cursorIndexOfShoppingOnlyStore);
            }
            final Double _tmpShoppingOnlyPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyPrice)) {
              _tmpShoppingOnlyPrice = null;
            } else {
              _tmpShoppingOnlyPrice = _cursor.getDouble(_cursorIndexOfShoppingOnlyPrice);
            }
            final String _tmpShoppingOnlyUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyUnit)) {
              _tmpShoppingOnlyUnit = null;
            } else {
              _tmpShoppingOnlyUnit = _cursor.getString(_cursorIndexOfShoppingOnlyUnit);
            }
            final String _tmpShoppingOnlyCategory;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyCategory)) {
              _tmpShoppingOnlyCategory = null;
            } else {
              _tmpShoppingOnlyCategory = _cursor.getString(_cursorIndexOfShoppingOnlyCategory);
            }
            final String _tmpShoppingOnlyImageUri;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyImageUri)) {
              _tmpShoppingOnlyImageUri = null;
            } else {
              _tmpShoppingOnlyImageUri = _cursor.getString(_cursorIndexOfShoppingOnlyImageUri);
            }
            final String _tmpVaultItemNameSnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemNameSnapshot)) {
              _tmpVaultItemNameSnapshot = null;
            } else {
              _tmpVaultItemNameSnapshot = _cursor.getString(_cursorIndexOfVaultItemNameSnapshot);
            }
            final String _tmpVaultItemCategorySnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemCategorySnapshot)) {
              _tmpVaultItemCategorySnapshot = null;
            } else {
              _tmpVaultItemCategorySnapshot = _cursor.getString(_cursorIndexOfVaultItemCategorySnapshot);
            }
            final Double _tmpOriginalPlanningQuantity;
            if (_cursor.isNull(_cursorIndexOfOriginalPlanningQuantity)) {
              _tmpOriginalPlanningQuantity = null;
            } else {
              _tmpOriginalPlanningQuantity = _cursor.getDouble(_cursorIndexOfOriginalPlanningQuantity);
            }
            final boolean _tmpAddedDuringShopping;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfAddedDuringShopping);
            _tmpAddedDuringShopping = _tmp_5 != 0;
            final int _tmpFulfillmentAnimationState;
            _tmpFulfillmentAnimationState = _cursor.getInt(_cursorIndexOfFulfillmentAnimationState);
            final Date _tmpFulfillmentStartTime;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfFulfillmentStartTime)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfFulfillmentStartTime);
            }
            _tmpFulfillmentStartTime = __converters.longToDate(_tmp_6);
            final boolean _tmpShouldShowCheckmark;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfShouldShowCheckmark);
            _tmpShouldShowCheckmark = _tmp_7 != 0;
            final boolean _tmpShouldStrikethrough;
            final int _tmp_8;
            _tmp_8 = _cursor.getInt(_cursorIndexOfShouldStrikethrough);
            _tmpShouldStrikethrough = _tmp_8 != 0;
            final boolean _tmpIsOnSale;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_9 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpSaleType;
            if (_cursor.isNull(_cursorIndexOfSaleType)) {
              _tmpSaleType = null;
            } else {
              _tmpSaleType = _cursor.getString(_cursorIndexOfSaleType);
            }
            final Double _tmpDiscountValue;
            if (_cursor.isNull(_cursorIndexOfDiscountValue)) {
              _tmpDiscountValue = null;
            } else {
              _tmpDiscountValue = _cursor.getDouble(_cursorIndexOfDiscountValue);
            }
            final Double _tmpRegularPrice;
            if (_cursor.isNull(_cursorIndexOfRegularPrice)) {
              _tmpRegularPrice = null;
            } else {
              _tmpRegularPrice = _cursor.getDouble(_cursorIndexOfRegularPrice);
            }
            _item = new CartItemEntity(_tmpUid,_tmpCartId,_tmpItemId,_tmpAddedAt,_tmpQuantity,_tmpIsFulfilled,_tmpIsSkippedDuringShopping,_tmpPlannedStore,_tmpPlannedPrice,_tmpPlannedUnit,_tmpActualStore,_tmpActualPrice,_tmpActualQuantity,_tmpActualUnit,_tmpWasEditedDuringShopping,_tmpIsShoppingOnlyItem,_tmpShoppingOnlyName,_tmpShoppingOnlyStore,_tmpShoppingOnlyPrice,_tmpShoppingOnlyUnit,_tmpShoppingOnlyCategory,_tmpShoppingOnlyImageUri,_tmpVaultItemNameSnapshot,_tmpVaultItemCategorySnapshot,_tmpOriginalPlanningQuantity,_tmpAddedDuringShopping,_tmpFulfillmentAnimationState,_tmpFulfillmentStartTime,_tmpShouldShowCheckmark,_tmpShouldStrikethrough,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object listForCarts(final List<String> cartIds,
      final Continuation<? super List<CartItemEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM cart_items WHERE cartId IN (");
    final int _inputSize = cartIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") ORDER BY cartId, addedAt ASC");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : cartIds) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CartItemEntity>>() {
      @Override
      @NonNull
      public List<CartItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfCartId = CursorUtil.getColumnIndexOrThrow(_cursor, "cartId");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfIsFulfilled = CursorUtil.getColumnIndexOrThrow(_cursor, "isFulfilled");
          final int _cursorIndexOfIsSkippedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "isSkippedDuringShopping");
          final int _cursorIndexOfPlannedStore = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedStore");
          final int _cursorIndexOfPlannedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedPrice");
          final int _cursorIndexOfPlannedUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedUnit");
          final int _cursorIndexOfActualStore = CursorUtil.getColumnIndexOrThrow(_cursor, "actualStore");
          final int _cursorIndexOfActualPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "actualPrice");
          final int _cursorIndexOfActualQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "actualQuantity");
          final int _cursorIndexOfActualUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "actualUnit");
          final int _cursorIndexOfWasEditedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "wasEditedDuringShopping");
          final int _cursorIndexOfIsShoppingOnlyItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isShoppingOnlyItem");
          final int _cursorIndexOfShoppingOnlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyName");
          final int _cursorIndexOfShoppingOnlyStore = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyStore");
          final int _cursorIndexOfShoppingOnlyPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyPrice");
          final int _cursorIndexOfShoppingOnlyUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyUnit");
          final int _cursorIndexOfShoppingOnlyCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyCategory");
          final int _cursorIndexOfShoppingOnlyImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyImageUri");
          final int _cursorIndexOfVaultItemNameSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemNameSnapshot");
          final int _cursorIndexOfVaultItemCategorySnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemCategorySnapshot");
          final int _cursorIndexOfOriginalPlanningQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPlanningQuantity");
          final int _cursorIndexOfAddedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "addedDuringShopping");
          final int _cursorIndexOfFulfillmentAnimationState = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentAnimationState");
          final int _cursorIndexOfFulfillmentStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStartTime");
          final int _cursorIndexOfShouldShowCheckmark = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldShowCheckmark");
          final int _cursorIndexOfShouldStrikethrough = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldStrikethrough");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final List<CartItemEntity> _result = new ArrayList<CartItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CartItemEntity _item_1;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpCartId;
            _tmpCartId = _cursor.getString(_cursorIndexOfCartId);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final Date _tmpAddedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _tmpAddedAt = __converters.longToDate(_tmp);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final boolean _tmpIsFulfilled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFulfilled);
            _tmpIsFulfilled = _tmp_1 != 0;
            final boolean _tmpIsSkippedDuringShopping;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsSkippedDuringShopping);
            _tmpIsSkippedDuringShopping = _tmp_2 != 0;
            final String _tmpPlannedStore;
            _tmpPlannedStore = _cursor.getString(_cursorIndexOfPlannedStore);
            final Double _tmpPlannedPrice;
            if (_cursor.isNull(_cursorIndexOfPlannedPrice)) {
              _tmpPlannedPrice = null;
            } else {
              _tmpPlannedPrice = _cursor.getDouble(_cursorIndexOfPlannedPrice);
            }
            final String _tmpPlannedUnit;
            if (_cursor.isNull(_cursorIndexOfPlannedUnit)) {
              _tmpPlannedUnit = null;
            } else {
              _tmpPlannedUnit = _cursor.getString(_cursorIndexOfPlannedUnit);
            }
            final String _tmpActualStore;
            if (_cursor.isNull(_cursorIndexOfActualStore)) {
              _tmpActualStore = null;
            } else {
              _tmpActualStore = _cursor.getString(_cursorIndexOfActualStore);
            }
            final Double _tmpActualPrice;
            if (_cursor.isNull(_cursorIndexOfActualPrice)) {
              _tmpActualPrice = null;
            } else {
              _tmpActualPrice = _cursor.getDouble(_cursorIndexOfActualPrice);
            }
            final Double _tmpActualQuantity;
            if (_cursor.isNull(_cursorIndexOfActualQuantity)) {
              _tmpActualQuantity = null;
            } else {
              _tmpActualQuantity = _cursor.getDouble(_cursorIndexOfActualQuantity);
            }
            final String _tmpActualUnit;
            if (_cursor.isNull(_cursorIndexOfActualUnit)) {
              _tmpActualUnit = null;
            } else {
              _tmpActualUnit = _cursor.getString(_cursorIndexOfActualUnit);
            }
            final boolean _tmpWasEditedDuringShopping;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWasEditedDuringShopping);
            _tmpWasEditedDuringShopping = _tmp_3 != 0;
            final boolean _tmpIsShoppingOnlyItem;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsShoppingOnlyItem);
            _tmpIsShoppingOnlyItem = _tmp_4 != 0;
            final String _tmpShoppingOnlyName;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyName)) {
              _tmpShoppingOnlyName = null;
            } else {
              _tmpShoppingOnlyName = _cursor.getString(_cursorIndexOfShoppingOnlyName);
            }
            final String _tmpShoppingOnlyStore;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyStore)) {
              _tmpShoppingOnlyStore = null;
            } else {
              _tmpShoppingOnlyStore = _cursor.getString(_cursorIndexOfShoppingOnlyStore);
            }
            final Double _tmpShoppingOnlyPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyPrice)) {
              _tmpShoppingOnlyPrice = null;
            } else {
              _tmpShoppingOnlyPrice = _cursor.getDouble(_cursorIndexOfShoppingOnlyPrice);
            }
            final String _tmpShoppingOnlyUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyUnit)) {
              _tmpShoppingOnlyUnit = null;
            } else {
              _tmpShoppingOnlyUnit = _cursor.getString(_cursorIndexOfShoppingOnlyUnit);
            }
            final String _tmpShoppingOnlyCategory;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyCategory)) {
              _tmpShoppingOnlyCategory = null;
            } else {
              _tmpShoppingOnlyCategory = _cursor.getString(_cursorIndexOfShoppingOnlyCategory);
            }
            final String _tmpShoppingOnlyImageUri;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyImageUri)) {
              _tmpShoppingOnlyImageUri = null;
            } else {
              _tmpShoppingOnlyImageUri = _cursor.getString(_cursorIndexOfShoppingOnlyImageUri);
            }
            final String _tmpVaultItemNameSnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemNameSnapshot)) {
              _tmpVaultItemNameSnapshot = null;
            } else {
              _tmpVaultItemNameSnapshot = _cursor.getString(_cursorIndexOfVaultItemNameSnapshot);
            }
            final String _tmpVaultItemCategorySnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemCategorySnapshot)) {
              _tmpVaultItemCategorySnapshot = null;
            } else {
              _tmpVaultItemCategorySnapshot = _cursor.getString(_cursorIndexOfVaultItemCategorySnapshot);
            }
            final Double _tmpOriginalPlanningQuantity;
            if (_cursor.isNull(_cursorIndexOfOriginalPlanningQuantity)) {
              _tmpOriginalPlanningQuantity = null;
            } else {
              _tmpOriginalPlanningQuantity = _cursor.getDouble(_cursorIndexOfOriginalPlanningQuantity);
            }
            final boolean _tmpAddedDuringShopping;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfAddedDuringShopping);
            _tmpAddedDuringShopping = _tmp_5 != 0;
            final int _tmpFulfillmentAnimationState;
            _tmpFulfillmentAnimationState = _cursor.getInt(_cursorIndexOfFulfillmentAnimationState);
            final Date _tmpFulfillmentStartTime;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfFulfillmentStartTime)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfFulfillmentStartTime);
            }
            _tmpFulfillmentStartTime = __converters.longToDate(_tmp_6);
            final boolean _tmpShouldShowCheckmark;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfShouldShowCheckmark);
            _tmpShouldShowCheckmark = _tmp_7 != 0;
            final boolean _tmpShouldStrikethrough;
            final int _tmp_8;
            _tmp_8 = _cursor.getInt(_cursorIndexOfShouldStrikethrough);
            _tmpShouldStrikethrough = _tmp_8 != 0;
            final boolean _tmpIsOnSale;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_9 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpSaleType;
            if (_cursor.isNull(_cursorIndexOfSaleType)) {
              _tmpSaleType = null;
            } else {
              _tmpSaleType = _cursor.getString(_cursorIndexOfSaleType);
            }
            final Double _tmpDiscountValue;
            if (_cursor.isNull(_cursorIndexOfDiscountValue)) {
              _tmpDiscountValue = null;
            } else {
              _tmpDiscountValue = _cursor.getDouble(_cursorIndexOfDiscountValue);
            }
            final Double _tmpRegularPrice;
            if (_cursor.isNull(_cursorIndexOfRegularPrice)) {
              _tmpRegularPrice = null;
            } else {
              _tmpRegularPrice = _cursor.getDouble(_cursorIndexOfRegularPrice);
            }
            _item_1 = new CartItemEntity(_tmpUid,_tmpCartId,_tmpItemId,_tmpAddedAt,_tmpQuantity,_tmpIsFulfilled,_tmpIsSkippedDuringShopping,_tmpPlannedStore,_tmpPlannedPrice,_tmpPlannedUnit,_tmpActualStore,_tmpActualPrice,_tmpActualQuantity,_tmpActualUnit,_tmpWasEditedDuringShopping,_tmpIsShoppingOnlyItem,_tmpShoppingOnlyName,_tmpShoppingOnlyStore,_tmpShoppingOnlyPrice,_tmpShoppingOnlyUnit,_tmpShoppingOnlyCategory,_tmpShoppingOnlyImageUri,_tmpVaultItemNameSnapshot,_tmpVaultItemCategorySnapshot,_tmpOriginalPlanningQuantity,_tmpAddedDuringShopping,_tmpFulfillmentAnimationState,_tmpFulfillmentStartTime,_tmpShouldShowCheckmark,_tmpShouldStrikethrough,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object findByCartAndItem(final String cartId, final String itemId,
      final Continuation<? super CartItemEntity> $completion) {
    final String _sql = "SELECT * FROM cart_items WHERE cartId = ? AND itemId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cartId);
    _argIndex = 2;
    _statement.bindString(_argIndex, itemId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CartItemEntity>() {
      @Override
      @Nullable
      public CartItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfCartId = CursorUtil.getColumnIndexOrThrow(_cursor, "cartId");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfIsFulfilled = CursorUtil.getColumnIndexOrThrow(_cursor, "isFulfilled");
          final int _cursorIndexOfIsSkippedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "isSkippedDuringShopping");
          final int _cursorIndexOfPlannedStore = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedStore");
          final int _cursorIndexOfPlannedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedPrice");
          final int _cursorIndexOfPlannedUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedUnit");
          final int _cursorIndexOfActualStore = CursorUtil.getColumnIndexOrThrow(_cursor, "actualStore");
          final int _cursorIndexOfActualPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "actualPrice");
          final int _cursorIndexOfActualQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "actualQuantity");
          final int _cursorIndexOfActualUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "actualUnit");
          final int _cursorIndexOfWasEditedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "wasEditedDuringShopping");
          final int _cursorIndexOfIsShoppingOnlyItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isShoppingOnlyItem");
          final int _cursorIndexOfShoppingOnlyName = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyName");
          final int _cursorIndexOfShoppingOnlyStore = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyStore");
          final int _cursorIndexOfShoppingOnlyPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyPrice");
          final int _cursorIndexOfShoppingOnlyUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyUnit");
          final int _cursorIndexOfShoppingOnlyCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyCategory");
          final int _cursorIndexOfShoppingOnlyImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingOnlyImageUri");
          final int _cursorIndexOfVaultItemNameSnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemNameSnapshot");
          final int _cursorIndexOfVaultItemCategorySnapshot = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultItemCategorySnapshot");
          final int _cursorIndexOfOriginalPlanningQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPlanningQuantity");
          final int _cursorIndexOfAddedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "addedDuringShopping");
          final int _cursorIndexOfFulfillmentAnimationState = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentAnimationState");
          final int _cursorIndexOfFulfillmentStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStartTime");
          final int _cursorIndexOfShouldShowCheckmark = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldShowCheckmark");
          final int _cursorIndexOfShouldStrikethrough = CursorUtil.getColumnIndexOrThrow(_cursor, "shouldStrikethrough");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final CartItemEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpCartId;
            _tmpCartId = _cursor.getString(_cursorIndexOfCartId);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final Date _tmpAddedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _tmpAddedAt = __converters.longToDate(_tmp);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final boolean _tmpIsFulfilled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFulfilled);
            _tmpIsFulfilled = _tmp_1 != 0;
            final boolean _tmpIsSkippedDuringShopping;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsSkippedDuringShopping);
            _tmpIsSkippedDuringShopping = _tmp_2 != 0;
            final String _tmpPlannedStore;
            _tmpPlannedStore = _cursor.getString(_cursorIndexOfPlannedStore);
            final Double _tmpPlannedPrice;
            if (_cursor.isNull(_cursorIndexOfPlannedPrice)) {
              _tmpPlannedPrice = null;
            } else {
              _tmpPlannedPrice = _cursor.getDouble(_cursorIndexOfPlannedPrice);
            }
            final String _tmpPlannedUnit;
            if (_cursor.isNull(_cursorIndexOfPlannedUnit)) {
              _tmpPlannedUnit = null;
            } else {
              _tmpPlannedUnit = _cursor.getString(_cursorIndexOfPlannedUnit);
            }
            final String _tmpActualStore;
            if (_cursor.isNull(_cursorIndexOfActualStore)) {
              _tmpActualStore = null;
            } else {
              _tmpActualStore = _cursor.getString(_cursorIndexOfActualStore);
            }
            final Double _tmpActualPrice;
            if (_cursor.isNull(_cursorIndexOfActualPrice)) {
              _tmpActualPrice = null;
            } else {
              _tmpActualPrice = _cursor.getDouble(_cursorIndexOfActualPrice);
            }
            final Double _tmpActualQuantity;
            if (_cursor.isNull(_cursorIndexOfActualQuantity)) {
              _tmpActualQuantity = null;
            } else {
              _tmpActualQuantity = _cursor.getDouble(_cursorIndexOfActualQuantity);
            }
            final String _tmpActualUnit;
            if (_cursor.isNull(_cursorIndexOfActualUnit)) {
              _tmpActualUnit = null;
            } else {
              _tmpActualUnit = _cursor.getString(_cursorIndexOfActualUnit);
            }
            final boolean _tmpWasEditedDuringShopping;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWasEditedDuringShopping);
            _tmpWasEditedDuringShopping = _tmp_3 != 0;
            final boolean _tmpIsShoppingOnlyItem;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsShoppingOnlyItem);
            _tmpIsShoppingOnlyItem = _tmp_4 != 0;
            final String _tmpShoppingOnlyName;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyName)) {
              _tmpShoppingOnlyName = null;
            } else {
              _tmpShoppingOnlyName = _cursor.getString(_cursorIndexOfShoppingOnlyName);
            }
            final String _tmpShoppingOnlyStore;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyStore)) {
              _tmpShoppingOnlyStore = null;
            } else {
              _tmpShoppingOnlyStore = _cursor.getString(_cursorIndexOfShoppingOnlyStore);
            }
            final Double _tmpShoppingOnlyPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyPrice)) {
              _tmpShoppingOnlyPrice = null;
            } else {
              _tmpShoppingOnlyPrice = _cursor.getDouble(_cursorIndexOfShoppingOnlyPrice);
            }
            final String _tmpShoppingOnlyUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyUnit)) {
              _tmpShoppingOnlyUnit = null;
            } else {
              _tmpShoppingOnlyUnit = _cursor.getString(_cursorIndexOfShoppingOnlyUnit);
            }
            final String _tmpShoppingOnlyCategory;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyCategory)) {
              _tmpShoppingOnlyCategory = null;
            } else {
              _tmpShoppingOnlyCategory = _cursor.getString(_cursorIndexOfShoppingOnlyCategory);
            }
            final String _tmpShoppingOnlyImageUri;
            if (_cursor.isNull(_cursorIndexOfShoppingOnlyImageUri)) {
              _tmpShoppingOnlyImageUri = null;
            } else {
              _tmpShoppingOnlyImageUri = _cursor.getString(_cursorIndexOfShoppingOnlyImageUri);
            }
            final String _tmpVaultItemNameSnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemNameSnapshot)) {
              _tmpVaultItemNameSnapshot = null;
            } else {
              _tmpVaultItemNameSnapshot = _cursor.getString(_cursorIndexOfVaultItemNameSnapshot);
            }
            final String _tmpVaultItemCategorySnapshot;
            if (_cursor.isNull(_cursorIndexOfVaultItemCategorySnapshot)) {
              _tmpVaultItemCategorySnapshot = null;
            } else {
              _tmpVaultItemCategorySnapshot = _cursor.getString(_cursorIndexOfVaultItemCategorySnapshot);
            }
            final Double _tmpOriginalPlanningQuantity;
            if (_cursor.isNull(_cursorIndexOfOriginalPlanningQuantity)) {
              _tmpOriginalPlanningQuantity = null;
            } else {
              _tmpOriginalPlanningQuantity = _cursor.getDouble(_cursorIndexOfOriginalPlanningQuantity);
            }
            final boolean _tmpAddedDuringShopping;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfAddedDuringShopping);
            _tmpAddedDuringShopping = _tmp_5 != 0;
            final int _tmpFulfillmentAnimationState;
            _tmpFulfillmentAnimationState = _cursor.getInt(_cursorIndexOfFulfillmentAnimationState);
            final Date _tmpFulfillmentStartTime;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfFulfillmentStartTime)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfFulfillmentStartTime);
            }
            _tmpFulfillmentStartTime = __converters.longToDate(_tmp_6);
            final boolean _tmpShouldShowCheckmark;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfShouldShowCheckmark);
            _tmpShouldShowCheckmark = _tmp_7 != 0;
            final boolean _tmpShouldStrikethrough;
            final int _tmp_8;
            _tmp_8 = _cursor.getInt(_cursorIndexOfShouldStrikethrough);
            _tmpShouldStrikethrough = _tmp_8 != 0;
            final boolean _tmpIsOnSale;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_9 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpSaleType;
            if (_cursor.isNull(_cursorIndexOfSaleType)) {
              _tmpSaleType = null;
            } else {
              _tmpSaleType = _cursor.getString(_cursorIndexOfSaleType);
            }
            final Double _tmpDiscountValue;
            if (_cursor.isNull(_cursorIndexOfDiscountValue)) {
              _tmpDiscountValue = null;
            } else {
              _tmpDiscountValue = _cursor.getDouble(_cursorIndexOfDiscountValue);
            }
            final Double _tmpRegularPrice;
            if (_cursor.isNull(_cursorIndexOfRegularPrice)) {
              _tmpRegularPrice = null;
            } else {
              _tmpRegularPrice = _cursor.getDouble(_cursorIndexOfRegularPrice);
            }
            _result = new CartItemEntity(_tmpUid,_tmpCartId,_tmpItemId,_tmpAddedAt,_tmpQuantity,_tmpIsFulfilled,_tmpIsSkippedDuringShopping,_tmpPlannedStore,_tmpPlannedPrice,_tmpPlannedUnit,_tmpActualStore,_tmpActualPrice,_tmpActualQuantity,_tmpActualUnit,_tmpWasEditedDuringShopping,_tmpIsShoppingOnlyItem,_tmpShoppingOnlyName,_tmpShoppingOnlyStore,_tmpShoppingOnlyPrice,_tmpShoppingOnlyUnit,_tmpShoppingOnlyCategory,_tmpShoppingOnlyImageUri,_tmpVaultItemNameSnapshot,_tmpVaultItemCategorySnapshot,_tmpOriginalPlanningQuantity,_tmpAddedDuringShopping,_tmpFulfillmentAnimationState,_tmpFulfillmentStartTime,_tmpShouldShowCheckmark,_tmpShouldStrikethrough,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
