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
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pocketsave.data.local.converter.Converters;
import com.pocketsave.data.local.entity.CartEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class CartDao_Impl implements CartDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CartEntity> __insertionAdapterOfCartEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<CartEntity> __deletionAdapterOfCartEntity;

  private final EntityDeletionOrUpdateAdapter<CartEntity> __updateAdapterOfCartEntity;

  public CartDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCartEntity = new EntityInsertionAdapter<CartEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `carts` (`id`,`vaultUid`,`name`,`budget`,`fulfillmentStatus`,`createdAt`,`updatedAt`,`startedAt`,`completedAt`,`status`,`isDeleted`,`deletedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CartEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getVaultUid());
        statement.bindString(3, entity.getName());
        statement.bindDouble(4, entity.getBudget());
        statement.bindDouble(5, entity.getFulfillmentStatus());
        final Long _tmp = __converters.dateToLong(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, _tmp);
        }
        final Long _tmp_1 = __converters.dateToLong(entity.getUpdatedAt());
        if (_tmp_1 == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, _tmp_1);
        }
        final Long _tmp_2 = __converters.dateToLong(entity.getStartedAt());
        if (_tmp_2 == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, _tmp_2);
        }
        final Long _tmp_3 = __converters.dateToLong(entity.getCompletedAt());
        if (_tmp_3 == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, _tmp_3);
        }
        statement.bindLong(10, entity.getStatus());
        final int _tmp_4 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp_4);
        final Long _tmp_5 = __converters.dateToLong(entity.getDeletedAt());
        if (_tmp_5 == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, _tmp_5);
        }
      }
    };
    this.__deletionAdapterOfCartEntity = new EntityDeletionOrUpdateAdapter<CartEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `carts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CartEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfCartEntity = new EntityDeletionOrUpdateAdapter<CartEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `carts` SET `id` = ?,`vaultUid` = ?,`name` = ?,`budget` = ?,`fulfillmentStatus` = ?,`createdAt` = ?,`updatedAt` = ?,`startedAt` = ?,`completedAt` = ?,`status` = ?,`isDeleted` = ?,`deletedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CartEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getVaultUid());
        statement.bindString(3, entity.getName());
        statement.bindDouble(4, entity.getBudget());
        statement.bindDouble(5, entity.getFulfillmentStatus());
        final Long _tmp = __converters.dateToLong(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, _tmp);
        }
        final Long _tmp_1 = __converters.dateToLong(entity.getUpdatedAt());
        if (_tmp_1 == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, _tmp_1);
        }
        final Long _tmp_2 = __converters.dateToLong(entity.getStartedAt());
        if (_tmp_2 == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, _tmp_2);
        }
        final Long _tmp_3 = __converters.dateToLong(entity.getCompletedAt());
        if (_tmp_3 == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, _tmp_3);
        }
        statement.bindLong(10, entity.getStatus());
        final int _tmp_4 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp_4);
        final Long _tmp_5 = __converters.dateToLong(entity.getDeletedAt());
        if (_tmp_5 == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, _tmp_5);
        }
        statement.bindString(13, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final CartEntity cart, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCartEntity.insert(cart);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final CartEntity cart, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCartEntity.handle(cart);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final CartEntity cart, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCartEntity.handle(cart);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CartEntity>> observeActive(final String vaultUid) {
    final String _sql = "SELECT * FROM carts WHERE vaultUid = ? AND isDeleted = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"carts"}, new Callable<List<CartEntity>>() {
      @Override
      @NonNull
      public List<CartEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "budget");
          final int _cursorIndexOfFulfillmentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStatus");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<CartEntity> _result = new ArrayList<CartEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CartEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpBudget;
            _tmpBudget = _cursor.getDouble(_cursorIndexOfBudget);
            final double _tmpFulfillmentStatus;
            _tmpFulfillmentStatus = _cursor.getDouble(_cursorIndexOfFulfillmentStatus);
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
            final Date _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Date _tmp_3 = __converters.longToDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final Date _tmpStartedAt;
            final Long _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            _tmpStartedAt = __converters.longToDate(_tmp_4);
            final Date _tmpCompletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _tmpCompletedAt = __converters.longToDate(_tmp_5);
            final int _tmpStatus;
            _tmpStatus = _cursor.getInt(_cursorIndexOfStatus);
            final boolean _tmpIsDeleted;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_6 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_7);
            _item = new CartEntity(_tmpId,_tmpVaultUid,_tmpName,_tmpBudget,_tmpFulfillmentStatus,_tmpCreatedAt,_tmpUpdatedAt,_tmpStartedAt,_tmpCompletedAt,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt);
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
  public Object listActiveByVault(final String vaultUid,
      final Continuation<? super List<CartEntity>> $completion) {
    final String _sql = "SELECT * FROM carts WHERE vaultUid = ? AND isDeleted = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CartEntity>>() {
      @Override
      @NonNull
      public List<CartEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "budget");
          final int _cursorIndexOfFulfillmentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStatus");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<CartEntity> _result = new ArrayList<CartEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CartEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpBudget;
            _tmpBudget = _cursor.getDouble(_cursorIndexOfBudget);
            final double _tmpFulfillmentStatus;
            _tmpFulfillmentStatus = _cursor.getDouble(_cursorIndexOfFulfillmentStatus);
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
            final Date _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Date _tmp_3 = __converters.longToDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final Date _tmpStartedAt;
            final Long _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            _tmpStartedAt = __converters.longToDate(_tmp_4);
            final Date _tmpCompletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _tmpCompletedAt = __converters.longToDate(_tmp_5);
            final int _tmpStatus;
            _tmpStatus = _cursor.getInt(_cursorIndexOfStatus);
            final boolean _tmpIsDeleted;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_6 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_7);
            _item = new CartEntity(_tmpId,_tmpVaultUid,_tmpName,_tmpBudget,_tmpFulfillmentStatus,_tmpCreatedAt,_tmpUpdatedAt,_tmpStartedAt,_tmpCompletedAt,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt);
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
      final Continuation<? super List<CartEntity>> $completion) {
    final String _sql = "SELECT * FROM carts WHERE vaultUid = ? AND isDeleted = 1 ORDER BY deletedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CartEntity>>() {
      @Override
      @NonNull
      public List<CartEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "budget");
          final int _cursorIndexOfFulfillmentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStatus");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final List<CartEntity> _result = new ArrayList<CartEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CartEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpBudget;
            _tmpBudget = _cursor.getDouble(_cursorIndexOfBudget);
            final double _tmpFulfillmentStatus;
            _tmpFulfillmentStatus = _cursor.getDouble(_cursorIndexOfFulfillmentStatus);
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
            final Date _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Date _tmp_3 = __converters.longToDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final Date _tmpStartedAt;
            final Long _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            _tmpStartedAt = __converters.longToDate(_tmp_4);
            final Date _tmpCompletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _tmpCompletedAt = __converters.longToDate(_tmp_5);
            final int _tmpStatus;
            _tmpStatus = _cursor.getInt(_cursorIndexOfStatus);
            final boolean _tmpIsDeleted;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_6 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_7);
            _item = new CartEntity(_tmpId,_tmpVaultUid,_tmpName,_tmpBudget,_tmpFulfillmentStatus,_tmpCreatedAt,_tmpUpdatedAt,_tmpStartedAt,_tmpCompletedAt,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt);
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
  public Object findById(final String id, final Continuation<? super CartEntity> $completion) {
    final String _sql = "SELECT * FROM carts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CartEntity>() {
      @Override
      @Nullable
      public CartEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "budget");
          final int _cursorIndexOfFulfillmentStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "fulfillmentStatus");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final CartEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpBudget;
            _tmpBudget = _cursor.getDouble(_cursorIndexOfBudget);
            final double _tmpFulfillmentStatus;
            _tmpFulfillmentStatus = _cursor.getDouble(_cursorIndexOfFulfillmentStatus);
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
            final Date _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Date _tmp_3 = __converters.longToDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final Date _tmpStartedAt;
            final Long _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            _tmpStartedAt = __converters.longToDate(_tmp_4);
            final Date _tmpCompletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _tmpCompletedAt = __converters.longToDate(_tmp_5);
            final int _tmpStatus;
            _tmpStatus = _cursor.getInt(_cursorIndexOfStatus);
            final boolean _tmpIsDeleted;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_6 != 0;
            final Date _tmpDeletedAt;
            final Long _tmp_7;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            _tmpDeletedAt = __converters.longToDate(_tmp_7);
            _result = new CartEntity(_tmpId,_tmpVaultUid,_tmpName,_tmpBudget,_tmpFulfillmentStatus,_tmpCreatedAt,_tmpUpdatedAt,_tmpStartedAt,_tmpCompletedAt,_tmpStatus,_tmpIsDeleted,_tmpDeletedAt);
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
