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
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pocketsave.data.local.converter.Converters;
import com.pocketsave.data.local.entity.ItemEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.IllegalStateException;
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
public final class ItemDao_Impl implements ItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ItemEntity> __insertionAdapterOfItemEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<ItemEntity> __deletionAdapterOfItemEntity;

  private final EntityDeletionOrUpdateAdapter<ItemEntity> __updateAdapterOfItemEntity;

  public ItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfItemEntity = new EntityInsertionAdapter<ItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `items` (`id`,`vaultUid`,`categoryUid`,`name`,`createdAt`,`isTemporaryShoppingItem`,`shoppingPrice`,`shoppingUnit`,`isOnSale`,`notes`,`saleType`,`discountValue`,`regularPrice`,`isDeleted`,`deletedAt`,`deletedFromCategoryName`,`isPlanSuppressed`,`planSuppressedAt`,`planSuppressedReason`,`imageUri`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getVaultUid());
        if (entity.getCategoryUid() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCategoryUid());
        }
        statement.bindString(4, entity.getName());
        final Long _tmp = __converters.dateToLong(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp);
        }
        final int _tmp_1 = entity.isTemporaryShoppingItem() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        if (entity.getShoppingPrice() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getShoppingPrice());
        }
        if (entity.getShoppingUnit() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getShoppingUnit());
        }
        final int _tmp_2 = entity.isOnSale() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        if (entity.getNotes() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getNotes());
        }
        if (entity.getSaleType() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getSaleType());
        }
        if (entity.getDiscountValue() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getDiscountValue());
        }
        if (entity.getRegularPrice() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getRegularPrice());
        }
        final int _tmp_3 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(14, _tmp_3);
        final Long _tmp_4 = __converters.dateToLong(entity.getDeletedAt());
        if (_tmp_4 == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, _tmp_4);
        }
        if (entity.getDeletedFromCategoryName() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getDeletedFromCategoryName());
        }
        final int _tmp_5 = entity.isPlanSuppressed() ? 1 : 0;
        statement.bindLong(17, _tmp_5);
        final Long _tmp_6 = __converters.dateToLong(entity.getPlanSuppressedAt());
        if (_tmp_6 == null) {
          statement.bindNull(18);
        } else {
          statement.bindLong(18, _tmp_6);
        }
        if (entity.getPlanSuppressedReason() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getPlanSuppressedReason());
        }
        if (entity.getImageUri() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getImageUri());
        }
      }
    };
    this.__deletionAdapterOfItemEntity = new EntityDeletionOrUpdateAdapter<ItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ItemEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfItemEntity = new EntityDeletionOrUpdateAdapter<ItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `items` SET `id` = ?,`vaultUid` = ?,`categoryUid` = ?,`name` = ?,`createdAt` = ?,`isTemporaryShoppingItem` = ?,`shoppingPrice` = ?,`shoppingUnit` = ?,`isOnSale` = ?,`notes` = ?,`saleType` = ?,`discountValue` = ?,`regularPrice` = ?,`isDeleted` = ?,`deletedAt` = ?,`deletedFromCategoryName` = ?,`isPlanSuppressed` = ?,`planSuppressedAt` = ?,`planSuppressedReason` = ?,`imageUri` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getVaultUid());
        if (entity.getCategoryUid() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCategoryUid());
        }
        statement.bindString(4, entity.getName());
        final Long _tmp = __converters.dateToLong(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp);
        }
        final int _tmp_1 = entity.isTemporaryShoppingItem() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        if (entity.getShoppingPrice() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getShoppingPrice());
        }
        if (entity.getShoppingUnit() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getShoppingUnit());
        }
        final int _tmp_2 = entity.isOnSale() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        if (entity.getNotes() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getNotes());
        }
        if (entity.getSaleType() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getSaleType());
        }
        if (entity.getDiscountValue() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getDiscountValue());
        }
        if (entity.getRegularPrice() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getRegularPrice());
        }
        final int _tmp_3 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(14, _tmp_3);
        final Long _tmp_4 = __converters.dateToLong(entity.getDeletedAt());
        if (_tmp_4 == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, _tmp_4);
        }
        if (entity.getDeletedFromCategoryName() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getDeletedFromCategoryName());
        }
        final int _tmp_5 = entity.isPlanSuppressed() ? 1 : 0;
        statement.bindLong(17, _tmp_5);
        final Long _tmp_6 = __converters.dateToLong(entity.getPlanSuppressedAt());
        if (_tmp_6 == null) {
          statement.bindNull(18);
        } else {
          statement.bindLong(18, _tmp_6);
        }
        if (entity.getPlanSuppressedReason() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getPlanSuppressedReason());
        }
        if (entity.getImageUri() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getImageUri());
        }
        statement.bindString(21, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final ItemEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfItemEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final ItemEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final ItemEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ItemEntity>> observeActive(final String vaultUid) {
    final String _sql = "SELECT * FROM items WHERE vaultUid = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfCategoryUid = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsTemporaryShoppingItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isTemporaryShoppingItem");
          final int _cursorIndexOfShoppingPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingPrice");
          final int _cursorIndexOfShoppingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingUnit");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfDeletedFromCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedFromCategoryName");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpCategoryUid;
            if (_cursor.isNull(_cursorIndexOfCategoryUid)) {
              _tmpCategoryUid = null;
            } else {
              _tmpCategoryUid = _cursor.getString(_cursorIndexOfCategoryUid);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.longToDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final boolean _tmpIsTemporaryShoppingItem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTemporaryShoppingItem);
            _tmpIsTemporaryShoppingItem = _tmp_2 != 0;
            final Double _tmpShoppingPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingPrice)) {
              _tmpShoppingPrice = null;
            } else {
              _tmpShoppingPrice = _cursor.getDouble(_cursorIndexOfShoppingPrice);
            }
            final String _tmpShoppingUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingUnit)) {
              _tmpShoppingUnit = null;
            } else {
              _tmpShoppingUnit = _cursor.getString(_cursorIndexOfShoppingUnit);
            }
            final boolean _tmpIsOnSale;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_3 != 0;
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
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_5);
            final String _tmpDeletedFromCategoryName;
            if (_cursor.isNull(_cursorIndexOfDeletedFromCategoryName)) {
              _tmpDeletedFromCategoryName = null;
            } else {
              _tmpDeletedFromCategoryName = _cursor.getString(_cursorIndexOfDeletedFromCategoryName);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp_6 != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_7);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _item = new ItemEntity(_tmpId,_tmpVaultUid,_tmpCategoryUid,_tmpName,_tmpCreatedAt,_tmpIsTemporaryShoppingItem,_tmpShoppingPrice,_tmpShoppingUnit,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice,_tmpIsDeleted,_tmpDeletedAt,_tmpDeletedFromCategoryName,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason,_tmpImageUri);
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
  public Object listActive(final String vaultUid,
      final Continuation<? super List<ItemEntity>> $completion) {
    final String _sql = "SELECT * FROM items WHERE vaultUid = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfCategoryUid = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsTemporaryShoppingItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isTemporaryShoppingItem");
          final int _cursorIndexOfShoppingPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingPrice");
          final int _cursorIndexOfShoppingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingUnit");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfDeletedFromCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedFromCategoryName");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpCategoryUid;
            if (_cursor.isNull(_cursorIndexOfCategoryUid)) {
              _tmpCategoryUid = null;
            } else {
              _tmpCategoryUid = _cursor.getString(_cursorIndexOfCategoryUid);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.longToDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final boolean _tmpIsTemporaryShoppingItem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTemporaryShoppingItem);
            _tmpIsTemporaryShoppingItem = _tmp_2 != 0;
            final Double _tmpShoppingPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingPrice)) {
              _tmpShoppingPrice = null;
            } else {
              _tmpShoppingPrice = _cursor.getDouble(_cursorIndexOfShoppingPrice);
            }
            final String _tmpShoppingUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingUnit)) {
              _tmpShoppingUnit = null;
            } else {
              _tmpShoppingUnit = _cursor.getString(_cursorIndexOfShoppingUnit);
            }
            final boolean _tmpIsOnSale;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_3 != 0;
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
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_5);
            final String _tmpDeletedFromCategoryName;
            if (_cursor.isNull(_cursorIndexOfDeletedFromCategoryName)) {
              _tmpDeletedFromCategoryName = null;
            } else {
              _tmpDeletedFromCategoryName = _cursor.getString(_cursorIndexOfDeletedFromCategoryName);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp_6 != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_7);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _item = new ItemEntity(_tmpId,_tmpVaultUid,_tmpCategoryUid,_tmpName,_tmpCreatedAt,_tmpIsTemporaryShoppingItem,_tmpShoppingPrice,_tmpShoppingUnit,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice,_tmpIsDeleted,_tmpDeletedAt,_tmpDeletedFromCategoryName,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason,_tmpImageUri);
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
  public Object listDeleted(final String vaultUid,
      final Continuation<? super List<ItemEntity>> $completion) {
    final String _sql = "SELECT * FROM items WHERE vaultUid = ? AND isDeleted = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfCategoryUid = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsTemporaryShoppingItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isTemporaryShoppingItem");
          final int _cursorIndexOfShoppingPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingPrice");
          final int _cursorIndexOfShoppingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingUnit");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfDeletedFromCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedFromCategoryName");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpCategoryUid;
            if (_cursor.isNull(_cursorIndexOfCategoryUid)) {
              _tmpCategoryUid = null;
            } else {
              _tmpCategoryUid = _cursor.getString(_cursorIndexOfCategoryUid);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.longToDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final boolean _tmpIsTemporaryShoppingItem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTemporaryShoppingItem);
            _tmpIsTemporaryShoppingItem = _tmp_2 != 0;
            final Double _tmpShoppingPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingPrice)) {
              _tmpShoppingPrice = null;
            } else {
              _tmpShoppingPrice = _cursor.getDouble(_cursorIndexOfShoppingPrice);
            }
            final String _tmpShoppingUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingUnit)) {
              _tmpShoppingUnit = null;
            } else {
              _tmpShoppingUnit = _cursor.getString(_cursorIndexOfShoppingUnit);
            }
            final boolean _tmpIsOnSale;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_3 != 0;
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
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_5);
            final String _tmpDeletedFromCategoryName;
            if (_cursor.isNull(_cursorIndexOfDeletedFromCategoryName)) {
              _tmpDeletedFromCategoryName = null;
            } else {
              _tmpDeletedFromCategoryName = _cursor.getString(_cursorIndexOfDeletedFromCategoryName);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp_6 != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_7);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _item = new ItemEntity(_tmpId,_tmpVaultUid,_tmpCategoryUid,_tmpName,_tmpCreatedAt,_tmpIsTemporaryShoppingItem,_tmpShoppingPrice,_tmpShoppingUnit,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice,_tmpIsDeleted,_tmpDeletedAt,_tmpDeletedFromCategoryName,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason,_tmpImageUri);
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
  public Object listByCategory(final String categoryUid,
      final Continuation<? super List<ItemEntity>> $completion) {
    final String _sql = "SELECT * FROM items WHERE categoryUid = ? AND isDeleted = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfCategoryUid = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsTemporaryShoppingItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isTemporaryShoppingItem");
          final int _cursorIndexOfShoppingPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingPrice");
          final int _cursorIndexOfShoppingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingUnit");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfDeletedFromCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedFromCategoryName");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpCategoryUid;
            if (_cursor.isNull(_cursorIndexOfCategoryUid)) {
              _tmpCategoryUid = null;
            } else {
              _tmpCategoryUid = _cursor.getString(_cursorIndexOfCategoryUid);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.longToDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final boolean _tmpIsTemporaryShoppingItem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTemporaryShoppingItem);
            _tmpIsTemporaryShoppingItem = _tmp_2 != 0;
            final Double _tmpShoppingPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingPrice)) {
              _tmpShoppingPrice = null;
            } else {
              _tmpShoppingPrice = _cursor.getDouble(_cursorIndexOfShoppingPrice);
            }
            final String _tmpShoppingUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingUnit)) {
              _tmpShoppingUnit = null;
            } else {
              _tmpShoppingUnit = _cursor.getString(_cursorIndexOfShoppingUnit);
            }
            final boolean _tmpIsOnSale;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_3 != 0;
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
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_5);
            final String _tmpDeletedFromCategoryName;
            if (_cursor.isNull(_cursorIndexOfDeletedFromCategoryName)) {
              _tmpDeletedFromCategoryName = null;
            } else {
              _tmpDeletedFromCategoryName = _cursor.getString(_cursorIndexOfDeletedFromCategoryName);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp_6 != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_7);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _item = new ItemEntity(_tmpId,_tmpVaultUid,_tmpCategoryUid,_tmpName,_tmpCreatedAt,_tmpIsTemporaryShoppingItem,_tmpShoppingPrice,_tmpShoppingUnit,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice,_tmpIsDeleted,_tmpDeletedAt,_tmpDeletedFromCategoryName,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason,_tmpImageUri);
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
  public Object findById(final String id, final Continuation<? super ItemEntity> $completion) {
    final String _sql = "SELECT * FROM items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ItemEntity>() {
      @Override
      @Nullable
      public ItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfCategoryUid = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsTemporaryShoppingItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isTemporaryShoppingItem");
          final int _cursorIndexOfShoppingPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingPrice");
          final int _cursorIndexOfShoppingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingUnit");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfDeletedFromCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedFromCategoryName");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final ItemEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpCategoryUid;
            if (_cursor.isNull(_cursorIndexOfCategoryUid)) {
              _tmpCategoryUid = null;
            } else {
              _tmpCategoryUid = _cursor.getString(_cursorIndexOfCategoryUid);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.longToDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final boolean _tmpIsTemporaryShoppingItem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTemporaryShoppingItem);
            _tmpIsTemporaryShoppingItem = _tmp_2 != 0;
            final Double _tmpShoppingPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingPrice)) {
              _tmpShoppingPrice = null;
            } else {
              _tmpShoppingPrice = _cursor.getDouble(_cursorIndexOfShoppingPrice);
            }
            final String _tmpShoppingUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingUnit)) {
              _tmpShoppingUnit = null;
            } else {
              _tmpShoppingUnit = _cursor.getString(_cursorIndexOfShoppingUnit);
            }
            final boolean _tmpIsOnSale;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_3 != 0;
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
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_5);
            final String _tmpDeletedFromCategoryName;
            if (_cursor.isNull(_cursorIndexOfDeletedFromCategoryName)) {
              _tmpDeletedFromCategoryName = null;
            } else {
              _tmpDeletedFromCategoryName = _cursor.getString(_cursorIndexOfDeletedFromCategoryName);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp_6 != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_7);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _result = new ItemEntity(_tmpId,_tmpVaultUid,_tmpCategoryUid,_tmpName,_tmpCreatedAt,_tmpIsTemporaryShoppingItem,_tmpShoppingPrice,_tmpShoppingUnit,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice,_tmpIsDeleted,_tmpDeletedAt,_tmpDeletedFromCategoryName,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason,_tmpImageUri);
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

  @Override
  public Object findByIds(final List<String> ids,
      final Continuation<? super List<ItemEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM items WHERE id IN (");
    final int _inputSize = ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : ids) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfCategoryUid = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsTemporaryShoppingItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isTemporaryShoppingItem");
          final int _cursorIndexOfShoppingPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingPrice");
          final int _cursorIndexOfShoppingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingUnit");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfDeletedFromCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedFromCategoryName");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item_1;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpCategoryUid;
            if (_cursor.isNull(_cursorIndexOfCategoryUid)) {
              _tmpCategoryUid = null;
            } else {
              _tmpCategoryUid = _cursor.getString(_cursorIndexOfCategoryUid);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.longToDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final boolean _tmpIsTemporaryShoppingItem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTemporaryShoppingItem);
            _tmpIsTemporaryShoppingItem = _tmp_2 != 0;
            final Double _tmpShoppingPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingPrice)) {
              _tmpShoppingPrice = null;
            } else {
              _tmpShoppingPrice = _cursor.getDouble(_cursorIndexOfShoppingPrice);
            }
            final String _tmpShoppingUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingUnit)) {
              _tmpShoppingUnit = null;
            } else {
              _tmpShoppingUnit = _cursor.getString(_cursorIndexOfShoppingUnit);
            }
            final boolean _tmpIsOnSale;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_3 != 0;
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
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_5);
            final String _tmpDeletedFromCategoryName;
            if (_cursor.isNull(_cursorIndexOfDeletedFromCategoryName)) {
              _tmpDeletedFromCategoryName = null;
            } else {
              _tmpDeletedFromCategoryName = _cursor.getString(_cursorIndexOfDeletedFromCategoryName);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp_6 != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_7);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _item_1 = new ItemEntity(_tmpId,_tmpVaultUid,_tmpCategoryUid,_tmpName,_tmpCreatedAt,_tmpIsTemporaryShoppingItem,_tmpShoppingPrice,_tmpShoppingUnit,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice,_tmpIsDeleted,_tmpDeletedAt,_tmpDeletedFromCategoryName,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason,_tmpImageUri);
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
  public Object findByNameExact(final String vaultUid, final String name,
      final Continuation<? super List<ItemEntity>> $completion) {
    final String _sql = "SELECT * FROM items WHERE vaultUid = ? AND LOWER(name) = LOWER(?) AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    _argIndex = 2;
    _statement.bindString(_argIndex, name);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfCategoryUid = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsTemporaryShoppingItem = CursorUtil.getColumnIndexOrThrow(_cursor, "isTemporaryShoppingItem");
          final int _cursorIndexOfShoppingPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingPrice");
          final int _cursorIndexOfShoppingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "shoppingUnit");
          final int _cursorIndexOfIsOnSale = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnSale");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfSaleType = CursorUtil.getColumnIndexOrThrow(_cursor, "saleType");
          final int _cursorIndexOfDiscountValue = CursorUtil.getColumnIndexOrThrow(_cursor, "discountValue");
          final int _cursorIndexOfRegularPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "regularPrice");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfDeletedFromCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedFromCategoryName");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpCategoryUid;
            if (_cursor.isNull(_cursorIndexOfCategoryUid)) {
              _tmpCategoryUid = null;
            } else {
              _tmpCategoryUid = _cursor.getString(_cursorIndexOfCategoryUid);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.longToDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final boolean _tmpIsTemporaryShoppingItem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsTemporaryShoppingItem);
            _tmpIsTemporaryShoppingItem = _tmp_2 != 0;
            final Double _tmpShoppingPrice;
            if (_cursor.isNull(_cursorIndexOfShoppingPrice)) {
              _tmpShoppingPrice = null;
            } else {
              _tmpShoppingPrice = _cursor.getDouble(_cursorIndexOfShoppingPrice);
            }
            final String _tmpShoppingUnit;
            if (_cursor.isNull(_cursorIndexOfShoppingUnit)) {
              _tmpShoppingUnit = null;
            } else {
              _tmpShoppingUnit = _cursor.getString(_cursorIndexOfShoppingUnit);
            }
            final boolean _tmpIsOnSale;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsOnSale);
            _tmpIsOnSale = _tmp_3 != 0;
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
            final boolean _tmpIsDeleted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_4 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_5);
            final String _tmpDeletedFromCategoryName;
            if (_cursor.isNull(_cursorIndexOfDeletedFromCategoryName)) {
              _tmpDeletedFromCategoryName = null;
            } else {
              _tmpDeletedFromCategoryName = _cursor.getString(_cursorIndexOfDeletedFromCategoryName);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp_6 != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_7);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _item = new ItemEntity(_tmpId,_tmpVaultUid,_tmpCategoryUid,_tmpName,_tmpCreatedAt,_tmpIsTemporaryShoppingItem,_tmpShoppingPrice,_tmpShoppingUnit,_tmpIsOnSale,_tmpNotes,_tmpSaleType,_tmpDiscountValue,_tmpRegularPrice,_tmpIsDeleted,_tmpDeletedAt,_tmpDeletedFromCategoryName,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason,_tmpImageUri);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
