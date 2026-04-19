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
import com.pocketsave.data.local.entity.PriceOptionEntity;
import com.pocketsave.data.local.entity.PricePerUnit;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PriceOptionDao_Impl implements PriceOptionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PriceOptionEntity> __insertionAdapterOfPriceOptionEntity;

  private final EntityDeletionOrUpdateAdapter<PriceOptionEntity> __deletionAdapterOfPriceOptionEntity;

  private final EntityDeletionOrUpdateAdapter<PriceOptionEntity> __updateAdapterOfPriceOptionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllForItem;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllForStoreInVault;

  public PriceOptionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPriceOptionEntity = new EntityInsertionAdapter<PriceOptionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `price_options` (`uid`,`itemId`,`store`,`priceValue`,`unit`,`packageSizeValue`,`packageSizeUnit`,`outerPackagingUnit`,`outerPackagingConfidence`,`outerPackagingSource`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PriceOptionEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getItemId());
        statement.bindString(3, entity.getStore());
        final PricePerUnit _tmpPricePerUnit = entity.getPricePerUnit();
        statement.bindDouble(4, _tmpPricePerUnit.getPriceValue());
        statement.bindString(5, _tmpPricePerUnit.getUnit());
        if (_tmpPricePerUnit.getPackageSizeValue() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, _tmpPricePerUnit.getPackageSizeValue());
        }
        if (_tmpPricePerUnit.getPackageSizeUnit() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmpPricePerUnit.getPackageSizeUnit());
        }
        if (_tmpPricePerUnit.getOuterPackagingUnit() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, _tmpPricePerUnit.getOuterPackagingUnit());
        }
        if (_tmpPricePerUnit.getOuterPackagingConfidence() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, _tmpPricePerUnit.getOuterPackagingConfidence());
        }
        if (_tmpPricePerUnit.getOuterPackagingSource() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, _tmpPricePerUnit.getOuterPackagingSource());
        }
      }
    };
    this.__deletionAdapterOfPriceOptionEntity = new EntityDeletionOrUpdateAdapter<PriceOptionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `price_options` WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PriceOptionEntity entity) {
        statement.bindString(1, entity.getUid());
      }
    };
    this.__updateAdapterOfPriceOptionEntity = new EntityDeletionOrUpdateAdapter<PriceOptionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `price_options` SET `uid` = ?,`itemId` = ?,`store` = ?,`priceValue` = ?,`unit` = ?,`packageSizeValue` = ?,`packageSizeUnit` = ?,`outerPackagingUnit` = ?,`outerPackagingConfidence` = ?,`outerPackagingSource` = ? WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PriceOptionEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getItemId());
        statement.bindString(3, entity.getStore());
        final PricePerUnit _tmpPricePerUnit = entity.getPricePerUnit();
        statement.bindDouble(4, _tmpPricePerUnit.getPriceValue());
        statement.bindString(5, _tmpPricePerUnit.getUnit());
        if (_tmpPricePerUnit.getPackageSizeValue() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, _tmpPricePerUnit.getPackageSizeValue());
        }
        if (_tmpPricePerUnit.getPackageSizeUnit() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmpPricePerUnit.getPackageSizeUnit());
        }
        if (_tmpPricePerUnit.getOuterPackagingUnit() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, _tmpPricePerUnit.getOuterPackagingUnit());
        }
        if (_tmpPricePerUnit.getOuterPackagingConfidence() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, _tmpPricePerUnit.getOuterPackagingConfidence());
        }
        if (_tmpPricePerUnit.getOuterPackagingSource() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, _tmpPricePerUnit.getOuterPackagingSource());
        }
        statement.bindString(11, entity.getUid());
      }
    };
    this.__preparedStmtOfDeleteAllForItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM price_options WHERE itemId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllForStoreInVault = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM price_options WHERE store = ? AND itemId IN (SELECT id FROM items WHERE vaultUid = ?)";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final PriceOptionEntity priceOption,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPriceOptionEntity.insert(priceOption);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final PriceOptionEntity priceOption,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPriceOptionEntity.handle(priceOption);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final PriceOptionEntity priceOption,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPriceOptionEntity.handle(priceOption);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllForItem(final String itemId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllForItem.acquire();
        int _argIndex = 1;
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
          __preparedStmtOfDeleteAllForItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllForStoreInVault(final String vaultUid, final String store,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllForStoreInVault.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, store);
        _argIndex = 2;
        _stmt.bindString(_argIndex, vaultUid);
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
          __preparedStmtOfDeleteAllForStoreInVault.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object listForItem(final String itemId,
      final Continuation<? super List<PriceOptionEntity>> $completion) {
    final String _sql = "SELECT * FROM price_options WHERE itemId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, itemId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PriceOptionEntity>>() {
      @Override
      @NonNull
      public List<PriceOptionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfStore = CursorUtil.getColumnIndexOrThrow(_cursor, "store");
          final int _cursorIndexOfPriceValue = CursorUtil.getColumnIndexOrThrow(_cursor, "priceValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfPackageSizeValue = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeValue");
          final int _cursorIndexOfPackageSizeUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeUnit");
          final int _cursorIndexOfOuterPackagingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingUnit");
          final int _cursorIndexOfOuterPackagingConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingConfidence");
          final int _cursorIndexOfOuterPackagingSource = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingSource");
          final List<PriceOptionEntity> _result = new ArrayList<PriceOptionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PriceOptionEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final String _tmpStore;
            _tmpStore = _cursor.getString(_cursorIndexOfStore);
            final PricePerUnit _tmpPricePerUnit;
            final double _tmpPriceValue;
            _tmpPriceValue = _cursor.getDouble(_cursorIndexOfPriceValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpPackageSizeValue;
            if (_cursor.isNull(_cursorIndexOfPackageSizeValue)) {
              _tmpPackageSizeValue = null;
            } else {
              _tmpPackageSizeValue = _cursor.getDouble(_cursorIndexOfPackageSizeValue);
            }
            final String _tmpPackageSizeUnit;
            if (_cursor.isNull(_cursorIndexOfPackageSizeUnit)) {
              _tmpPackageSizeUnit = null;
            } else {
              _tmpPackageSizeUnit = _cursor.getString(_cursorIndexOfPackageSizeUnit);
            }
            final String _tmpOuterPackagingUnit;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingUnit)) {
              _tmpOuterPackagingUnit = null;
            } else {
              _tmpOuterPackagingUnit = _cursor.getString(_cursorIndexOfOuterPackagingUnit);
            }
            final Double _tmpOuterPackagingConfidence;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingConfidence)) {
              _tmpOuterPackagingConfidence = null;
            } else {
              _tmpOuterPackagingConfidence = _cursor.getDouble(_cursorIndexOfOuterPackagingConfidence);
            }
            final String _tmpOuterPackagingSource;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingSource)) {
              _tmpOuterPackagingSource = null;
            } else {
              _tmpOuterPackagingSource = _cursor.getString(_cursorIndexOfOuterPackagingSource);
            }
            _tmpPricePerUnit = new PricePerUnit(_tmpPriceValue,_tmpUnit,_tmpPackageSizeValue,_tmpPackageSizeUnit,_tmpOuterPackagingUnit,_tmpOuterPackagingConfidence,_tmpOuterPackagingSource);
            _item = new PriceOptionEntity(_tmpUid,_tmpItemId,_tmpStore,_tmpPricePerUnit);
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
  public Object listForItems(final List<String> itemIds,
      final Continuation<? super List<PriceOptionEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM price_options WHERE itemId IN (");
    final int _inputSize = itemIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : itemIds) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PriceOptionEntity>>() {
      @Override
      @NonNull
      public List<PriceOptionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfStore = CursorUtil.getColumnIndexOrThrow(_cursor, "store");
          final int _cursorIndexOfPriceValue = CursorUtil.getColumnIndexOrThrow(_cursor, "priceValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfPackageSizeValue = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeValue");
          final int _cursorIndexOfPackageSizeUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeUnit");
          final int _cursorIndexOfOuterPackagingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingUnit");
          final int _cursorIndexOfOuterPackagingConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingConfidence");
          final int _cursorIndexOfOuterPackagingSource = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingSource");
          final List<PriceOptionEntity> _result = new ArrayList<PriceOptionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PriceOptionEntity _item_1;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final String _tmpStore;
            _tmpStore = _cursor.getString(_cursorIndexOfStore);
            final PricePerUnit _tmpPricePerUnit;
            final double _tmpPriceValue;
            _tmpPriceValue = _cursor.getDouble(_cursorIndexOfPriceValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpPackageSizeValue;
            if (_cursor.isNull(_cursorIndexOfPackageSizeValue)) {
              _tmpPackageSizeValue = null;
            } else {
              _tmpPackageSizeValue = _cursor.getDouble(_cursorIndexOfPackageSizeValue);
            }
            final String _tmpPackageSizeUnit;
            if (_cursor.isNull(_cursorIndexOfPackageSizeUnit)) {
              _tmpPackageSizeUnit = null;
            } else {
              _tmpPackageSizeUnit = _cursor.getString(_cursorIndexOfPackageSizeUnit);
            }
            final String _tmpOuterPackagingUnit;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingUnit)) {
              _tmpOuterPackagingUnit = null;
            } else {
              _tmpOuterPackagingUnit = _cursor.getString(_cursorIndexOfOuterPackagingUnit);
            }
            final Double _tmpOuterPackagingConfidence;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingConfidence)) {
              _tmpOuterPackagingConfidence = null;
            } else {
              _tmpOuterPackagingConfidence = _cursor.getDouble(_cursorIndexOfOuterPackagingConfidence);
            }
            final String _tmpOuterPackagingSource;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingSource)) {
              _tmpOuterPackagingSource = null;
            } else {
              _tmpOuterPackagingSource = _cursor.getString(_cursorIndexOfOuterPackagingSource);
            }
            _tmpPricePerUnit = new PricePerUnit(_tmpPriceValue,_tmpUnit,_tmpPackageSizeValue,_tmpPackageSizeUnit,_tmpOuterPackagingUnit,_tmpOuterPackagingConfidence,_tmpOuterPackagingSource);
            _item_1 = new PriceOptionEntity(_tmpUid,_tmpItemId,_tmpStore,_tmpPricePerUnit);
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
  public Object findByItemAndStore(final String itemId, final String store,
      final Continuation<? super PriceOptionEntity> $completion) {
    final String _sql = "SELECT * FROM price_options WHERE itemId = ? AND store = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, itemId);
    _argIndex = 2;
    _statement.bindString(_argIndex, store);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PriceOptionEntity>() {
      @Override
      @Nullable
      public PriceOptionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfStore = CursorUtil.getColumnIndexOrThrow(_cursor, "store");
          final int _cursorIndexOfPriceValue = CursorUtil.getColumnIndexOrThrow(_cursor, "priceValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfPackageSizeValue = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeValue");
          final int _cursorIndexOfPackageSizeUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeUnit");
          final int _cursorIndexOfOuterPackagingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingUnit");
          final int _cursorIndexOfOuterPackagingConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingConfidence");
          final int _cursorIndexOfOuterPackagingSource = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingSource");
          final PriceOptionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final String _tmpStore;
            _tmpStore = _cursor.getString(_cursorIndexOfStore);
            final PricePerUnit _tmpPricePerUnit;
            final double _tmpPriceValue;
            _tmpPriceValue = _cursor.getDouble(_cursorIndexOfPriceValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpPackageSizeValue;
            if (_cursor.isNull(_cursorIndexOfPackageSizeValue)) {
              _tmpPackageSizeValue = null;
            } else {
              _tmpPackageSizeValue = _cursor.getDouble(_cursorIndexOfPackageSizeValue);
            }
            final String _tmpPackageSizeUnit;
            if (_cursor.isNull(_cursorIndexOfPackageSizeUnit)) {
              _tmpPackageSizeUnit = null;
            } else {
              _tmpPackageSizeUnit = _cursor.getString(_cursorIndexOfPackageSizeUnit);
            }
            final String _tmpOuterPackagingUnit;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingUnit)) {
              _tmpOuterPackagingUnit = null;
            } else {
              _tmpOuterPackagingUnit = _cursor.getString(_cursorIndexOfOuterPackagingUnit);
            }
            final Double _tmpOuterPackagingConfidence;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingConfidence)) {
              _tmpOuterPackagingConfidence = null;
            } else {
              _tmpOuterPackagingConfidence = _cursor.getDouble(_cursorIndexOfOuterPackagingConfidence);
            }
            final String _tmpOuterPackagingSource;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingSource)) {
              _tmpOuterPackagingSource = null;
            } else {
              _tmpOuterPackagingSource = _cursor.getString(_cursorIndexOfOuterPackagingSource);
            }
            _tmpPricePerUnit = new PricePerUnit(_tmpPriceValue,_tmpUnit,_tmpPackageSizeValue,_tmpPackageSizeUnit,_tmpOuterPackagingUnit,_tmpOuterPackagingConfidence,_tmpOuterPackagingSource);
            _result = new PriceOptionEntity(_tmpUid,_tmpItemId,_tmpStore,_tmpPricePerUnit);
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
  public Object countDuplicateByNameAndStore(final String vaultUid, final String name,
      final String store, final String excludingItemId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM price_options p INNER JOIN items i ON p.itemId = i.id WHERE i.vaultUid = ? AND i.isDeleted = 0 AND LOWER(TRIM(i.name)) = LOWER(TRIM(?)) AND LOWER(TRIM(p.store)) = LOWER(TRIM(?)) AND (? IS NULL OR i.id != ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 5);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    _argIndex = 2;
    _statement.bindString(_argIndex, name);
    _argIndex = 3;
    _statement.bindString(_argIndex, store);
    _argIndex = 4;
    if (excludingItemId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, excludingItemId);
    }
    _argIndex = 5;
    if (excludingItemId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, excludingItemId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object listActiveByVault(final String vaultUid,
      final Continuation<? super List<PriceOptionEntity>> $completion) {
    final String _sql = "SELECT p.* FROM price_options p INNER JOIN items i ON p.itemId = i.id WHERE i.vaultUid = ? AND i.isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PriceOptionEntity>>() {
      @Override
      @NonNull
      public List<PriceOptionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfStore = CursorUtil.getColumnIndexOrThrow(_cursor, "store");
          final int _cursorIndexOfPriceValue = CursorUtil.getColumnIndexOrThrow(_cursor, "priceValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfPackageSizeValue = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeValue");
          final int _cursorIndexOfPackageSizeUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "packageSizeUnit");
          final int _cursorIndexOfOuterPackagingUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingUnit");
          final int _cursorIndexOfOuterPackagingConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingConfidence");
          final int _cursorIndexOfOuterPackagingSource = CursorUtil.getColumnIndexOrThrow(_cursor, "outerPackagingSource");
          final List<PriceOptionEntity> _result = new ArrayList<PriceOptionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PriceOptionEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final String _tmpStore;
            _tmpStore = _cursor.getString(_cursorIndexOfStore);
            final PricePerUnit _tmpPricePerUnit;
            final double _tmpPriceValue;
            _tmpPriceValue = _cursor.getDouble(_cursorIndexOfPriceValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpPackageSizeValue;
            if (_cursor.isNull(_cursorIndexOfPackageSizeValue)) {
              _tmpPackageSizeValue = null;
            } else {
              _tmpPackageSizeValue = _cursor.getDouble(_cursorIndexOfPackageSizeValue);
            }
            final String _tmpPackageSizeUnit;
            if (_cursor.isNull(_cursorIndexOfPackageSizeUnit)) {
              _tmpPackageSizeUnit = null;
            } else {
              _tmpPackageSizeUnit = _cursor.getString(_cursorIndexOfPackageSizeUnit);
            }
            final String _tmpOuterPackagingUnit;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingUnit)) {
              _tmpOuterPackagingUnit = null;
            } else {
              _tmpOuterPackagingUnit = _cursor.getString(_cursorIndexOfOuterPackagingUnit);
            }
            final Double _tmpOuterPackagingConfidence;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingConfidence)) {
              _tmpOuterPackagingConfidence = null;
            } else {
              _tmpOuterPackagingConfidence = _cursor.getDouble(_cursorIndexOfOuterPackagingConfidence);
            }
            final String _tmpOuterPackagingSource;
            if (_cursor.isNull(_cursorIndexOfOuterPackagingSource)) {
              _tmpOuterPackagingSource = null;
            } else {
              _tmpOuterPackagingSource = _cursor.getString(_cursorIndexOfOuterPackagingSource);
            }
            _tmpPricePerUnit = new PricePerUnit(_tmpPriceValue,_tmpUnit,_tmpPackageSizeValue,_tmpPackageSizeUnit,_tmpOuterPackagingUnit,_tmpOuterPackagingConfidence,_tmpOuterPackagingSource);
            _item = new PriceOptionEntity(_tmpUid,_tmpItemId,_tmpStore,_tmpPricePerUnit);
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
