package com.pocketsave.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pocketsave.data.local.entity.DeletedCartItemSnapshotEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class DeletedCartItemSnapshotDao_Impl implements DeletedCartItemSnapshotDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DeletedCartItemSnapshotEntity> __insertionAdapterOfDeletedCartItemSnapshotEntity;

  private final EntityDeletionOrUpdateAdapter<DeletedCartItemSnapshotEntity> __deletionAdapterOfDeletedCartItemSnapshotEntity;

  public DeletedCartItemSnapshotDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDeletedCartItemSnapshotEntity = new EntityInsertionAdapter<DeletedCartItemSnapshotEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `deleted_cart_item_snapshots` (`uid`,`cartId`,`itemId`,`quantity`,`plannedStore`,`plannedPrice`,`plannedUnit`,`actualStore`,`actualPrice`,`actualQuantity`,`actualUnit`,`wasEditedDuringShopping`,`wasFulfilled`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeletedCartItemSnapshotEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getCartId());
        if (entity.getItemId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getItemId());
        }
        statement.bindDouble(4, entity.getQuantity());
        statement.bindString(5, entity.getPlannedStore());
        if (entity.getPlannedPrice() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getPlannedPrice());
        }
        if (entity.getPlannedUnit() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPlannedUnit());
        }
        if (entity.getActualStore() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getActualStore());
        }
        if (entity.getActualPrice() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getActualPrice());
        }
        if (entity.getActualQuantity() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getActualQuantity());
        }
        if (entity.getActualUnit() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getActualUnit());
        }
        final int _tmp = entity.getWasEditedDuringShopping() ? 1 : 0;
        statement.bindLong(12, _tmp);
        final int _tmp_1 = entity.getWasFulfilled() ? 1 : 0;
        statement.bindLong(13, _tmp_1);
      }
    };
    this.__deletionAdapterOfDeletedCartItemSnapshotEntity = new EntityDeletionOrUpdateAdapter<DeletedCartItemSnapshotEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `deleted_cart_item_snapshots` WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DeletedCartItemSnapshotEntity entity) {
        statement.bindString(1, entity.getUid());
      }
    };
  }

  @Override
  public Object insert(final DeletedCartItemSnapshotEntity snapshot,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDeletedCartItemSnapshotEntity.insert(snapshot);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final DeletedCartItemSnapshotEntity snapshot,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDeletedCartItemSnapshotEntity.handle(snapshot);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object listByCart(final String cartId,
      final Continuation<? super List<DeletedCartItemSnapshotEntity>> $completion) {
    final String _sql = "SELECT * FROM deleted_cart_item_snapshots WHERE cartId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cartId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DeletedCartItemSnapshotEntity>>() {
      @Override
      @NonNull
      public List<DeletedCartItemSnapshotEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfCartId = CursorUtil.getColumnIndexOrThrow(_cursor, "cartId");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPlannedStore = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedStore");
          final int _cursorIndexOfPlannedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedPrice");
          final int _cursorIndexOfPlannedUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedUnit");
          final int _cursorIndexOfActualStore = CursorUtil.getColumnIndexOrThrow(_cursor, "actualStore");
          final int _cursorIndexOfActualPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "actualPrice");
          final int _cursorIndexOfActualQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "actualQuantity");
          final int _cursorIndexOfActualUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "actualUnit");
          final int _cursorIndexOfWasEditedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "wasEditedDuringShopping");
          final int _cursorIndexOfWasFulfilled = CursorUtil.getColumnIndexOrThrow(_cursor, "wasFulfilled");
          final List<DeletedCartItemSnapshotEntity> _result = new ArrayList<DeletedCartItemSnapshotEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeletedCartItemSnapshotEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpCartId;
            _tmpCartId = _cursor.getString(_cursorIndexOfCartId);
            final String _tmpItemId;
            if (_cursor.isNull(_cursorIndexOfItemId)) {
              _tmpItemId = null;
            } else {
              _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            }
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
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
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfWasEditedDuringShopping);
            _tmpWasEditedDuringShopping = _tmp != 0;
            final boolean _tmpWasFulfilled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfWasFulfilled);
            _tmpWasFulfilled = _tmp_1 != 0;
            _item = new DeletedCartItemSnapshotEntity(_tmpUid,_tmpCartId,_tmpItemId,_tmpQuantity,_tmpPlannedStore,_tmpPlannedPrice,_tmpPlannedUnit,_tmpActualStore,_tmpActualPrice,_tmpActualQuantity,_tmpActualUnit,_tmpWasEditedDuringShopping,_tmpWasFulfilled);
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
  public Object listByItem(final String itemId,
      final Continuation<? super List<DeletedCartItemSnapshotEntity>> $completion) {
    final String _sql = "SELECT * FROM deleted_cart_item_snapshots WHERE itemId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, itemId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DeletedCartItemSnapshotEntity>>() {
      @Override
      @NonNull
      public List<DeletedCartItemSnapshotEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfCartId = CursorUtil.getColumnIndexOrThrow(_cursor, "cartId");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPlannedStore = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedStore");
          final int _cursorIndexOfPlannedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedPrice");
          final int _cursorIndexOfPlannedUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedUnit");
          final int _cursorIndexOfActualStore = CursorUtil.getColumnIndexOrThrow(_cursor, "actualStore");
          final int _cursorIndexOfActualPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "actualPrice");
          final int _cursorIndexOfActualQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "actualQuantity");
          final int _cursorIndexOfActualUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "actualUnit");
          final int _cursorIndexOfWasEditedDuringShopping = CursorUtil.getColumnIndexOrThrow(_cursor, "wasEditedDuringShopping");
          final int _cursorIndexOfWasFulfilled = CursorUtil.getColumnIndexOrThrow(_cursor, "wasFulfilled");
          final List<DeletedCartItemSnapshotEntity> _result = new ArrayList<DeletedCartItemSnapshotEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeletedCartItemSnapshotEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpCartId;
            _tmpCartId = _cursor.getString(_cursorIndexOfCartId);
            final String _tmpItemId;
            if (_cursor.isNull(_cursorIndexOfItemId)) {
              _tmpItemId = null;
            } else {
              _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            }
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
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
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfWasEditedDuringShopping);
            _tmpWasEditedDuringShopping = _tmp != 0;
            final boolean _tmpWasFulfilled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfWasFulfilled);
            _tmpWasFulfilled = _tmp_1 != 0;
            _item = new DeletedCartItemSnapshotEntity(_tmpUid,_tmpCartId,_tmpItemId,_tmpQuantity,_tmpPlannedStore,_tmpPlannedPrice,_tmpPlannedUnit,_tmpActualStore,_tmpActualPrice,_tmpActualQuantity,_tmpActualUnit,_tmpWasEditedDuringShopping,_tmpWasFulfilled);
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
