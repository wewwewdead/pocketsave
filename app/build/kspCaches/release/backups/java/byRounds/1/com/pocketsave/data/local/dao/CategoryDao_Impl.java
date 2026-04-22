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
import com.pocketsave.data.local.entity.CategoryEntity;
import java.lang.Class;
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
public final class CategoryDao_Impl implements CategoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CategoryEntity> __insertionAdapterOfCategoryEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<CategoryEntity> __deletionAdapterOfCategoryEntity;

  private final EntityDeletionOrUpdateAdapter<CategoryEntity> __updateAdapterOfCategoryEntity;

  public CategoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCategoryEntity = new EntityInsertionAdapter<CategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `categories` (`uid`,`vaultUid`,`name`,`iconKey`,`sortOrder`,`colorHex`,`isPlanSuppressed`,`planSuppressedAt`,`planSuppressedReason`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CategoryEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getVaultUid());
        statement.bindString(3, entity.getName());
        if (entity.getIconKey() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIconKey());
        }
        statement.bindLong(5, entity.getSortOrder());
        if (entity.getColorHex() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getColorHex());
        }
        final int _tmp = entity.isPlanSuppressed() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final Long _tmp_1 = __converters.dateToLong(entity.getPlanSuppressedAt());
        if (_tmp_1 == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, _tmp_1);
        }
        if (entity.getPlanSuppressedReason() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPlanSuppressedReason());
        }
      }
    };
    this.__deletionAdapterOfCategoryEntity = new EntityDeletionOrUpdateAdapter<CategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `categories` WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CategoryEntity entity) {
        statement.bindString(1, entity.getUid());
      }
    };
    this.__updateAdapterOfCategoryEntity = new EntityDeletionOrUpdateAdapter<CategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `categories` SET `uid` = ?,`vaultUid` = ?,`name` = ?,`iconKey` = ?,`sortOrder` = ?,`colorHex` = ?,`isPlanSuppressed` = ?,`planSuppressedAt` = ?,`planSuppressedReason` = ? WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CategoryEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getVaultUid());
        statement.bindString(3, entity.getName());
        if (entity.getIconKey() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIconKey());
        }
        statement.bindLong(5, entity.getSortOrder());
        if (entity.getColorHex() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getColorHex());
        }
        final int _tmp = entity.isPlanSuppressed() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final Long _tmp_1 = __converters.dateToLong(entity.getPlanSuppressedAt());
        if (_tmp_1 == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, _tmp_1);
        }
        if (entity.getPlanSuppressedReason() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPlanSuppressedReason());
        }
        statement.bindString(10, entity.getUid());
      }
    };
  }

  @Override
  public Object insert(final CategoryEntity category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCategoryEntity.insert(category);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<CategoryEntity> categories,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCategoryEntity.insert(categories);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final CategoryEntity category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCategoryEntity.handle(category);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final CategoryEntity category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCategoryEntity.handle(category);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CategoryEntity>> observeByVault(final String vaultUid) {
    final String _sql = "SELECT * FROM categories WHERE vaultUid = ? ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"categories"}, new Callable<List<CategoryEntity>>() {
      @Override
      @NonNull
      public List<CategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final List<CategoryEntity> _result = new ArrayList<CategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIconKey;
            if (_cursor.isNull(_cursorIndexOfIconKey)) {
              _tmpIconKey = null;
            } else {
              _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_1);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            _item = new CategoryEntity(_tmpUid,_tmpVaultUid,_tmpName,_tmpIconKey,_tmpSortOrder,_tmpColorHex,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason);
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
  public Object listByVault(final String vaultUid,
      final Continuation<? super List<CategoryEntity>> $completion) {
    final String _sql = "SELECT * FROM categories WHERE vaultUid = ? ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CategoryEntity>>() {
      @Override
      @NonNull
      public List<CategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final List<CategoryEntity> _result = new ArrayList<CategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIconKey;
            if (_cursor.isNull(_cursorIndexOfIconKey)) {
              _tmpIconKey = null;
            } else {
              _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_1);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            _item = new CategoryEntity(_tmpUid,_tmpVaultUid,_tmpName,_tmpIconKey,_tmpSortOrder,_tmpColorHex,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason);
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
  public Object findByUid(final String uid,
      final Continuation<? super CategoryEntity> $completion) {
    final String _sql = "SELECT * FROM categories WHERE uid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CategoryEntity>() {
      @Override
      @Nullable
      public CategoryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final CategoryEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIconKey;
            if (_cursor.isNull(_cursorIndexOfIconKey)) {
              _tmpIconKey = null;
            } else {
              _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_1);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            _result = new CategoryEntity(_tmpUid,_tmpVaultUid,_tmpName,_tmpIconKey,_tmpSortOrder,_tmpColorHex,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason);
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
  public Object findByUids(final List<String> uids,
      final Continuation<? super List<CategoryEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM categories WHERE uid IN (");
    final int _inputSize = uids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : uids) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CategoryEntity>>() {
      @Override
      @NonNull
      public List<CategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final List<CategoryEntity> _result = new ArrayList<CategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryEntity _item_1;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIconKey;
            if (_cursor.isNull(_cursorIndexOfIconKey)) {
              _tmpIconKey = null;
            } else {
              _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_1);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            _item_1 = new CategoryEntity(_tmpUid,_tmpVaultUid,_tmpName,_tmpIconKey,_tmpSortOrder,_tmpColorHex,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason);
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
  public Object findByName(final String vaultUid, final String name,
      final Continuation<? super CategoryEntity> $completion) {
    final String _sql = "SELECT * FROM categories WHERE vaultUid = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?)) LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, vaultUid);
    _argIndex = 2;
    _statement.bindString(_argIndex, name);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CategoryEntity>() {
      @Override
      @Nullable
      public CategoryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfVaultUid = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIconKey = CursorUtil.getColumnIndexOrThrow(_cursor, "iconKey");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsPlanSuppressed = CursorUtil.getColumnIndexOrThrow(_cursor, "isPlanSuppressed");
          final int _cursorIndexOfPlanSuppressedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedAt");
          final int _cursorIndexOfPlanSuppressedReason = CursorUtil.getColumnIndexOrThrow(_cursor, "planSuppressedReason");
          final CategoryEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpVaultUid;
            _tmpVaultUid = _cursor.getString(_cursorIndexOfVaultUid);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIconKey;
            if (_cursor.isNull(_cursorIndexOfIconKey)) {
              _tmpIconKey = null;
            } else {
              _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            final boolean _tmpIsPlanSuppressed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPlanSuppressed);
            _tmpIsPlanSuppressed = _tmp != 0;
            final Date _tmpPlanSuppressedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfPlanSuppressedAt);
            }
            _tmpPlanSuppressedAt = __converters.longToDate(_tmp_1);
            final String _tmpPlanSuppressedReason;
            if (_cursor.isNull(_cursorIndexOfPlanSuppressedReason)) {
              _tmpPlanSuppressedReason = null;
            } else {
              _tmpPlanSuppressedReason = _cursor.getString(_cursorIndexOfPlanSuppressedReason);
            }
            _result = new CategoryEntity(_tmpUid,_tmpVaultUid,_tmpName,_tmpIconKey,_tmpSortOrder,_tmpColorHex,_tmpIsPlanSuppressed,_tmpPlanSuppressedAt,_tmpPlanSuppressedReason);
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
