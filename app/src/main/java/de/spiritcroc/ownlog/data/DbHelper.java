/*
 * Copyright (C) 2017-2018 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.ownlog.data;


import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.Nullable;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.List;

import de.spiritcroc.ownlog.FileHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = DbHelper.class.getSimpleName();

    private static boolean INITIALIZED = false;

    public static final int VERSION = 6;
    public static final String NAME = "log.db";
    public static final String NAME_COPY = "log2.db";
    @Deprecated
    private static final String NAME_BACKUP = "log_backup.db";

    private static final String TEXT = " TEXT";
    private static final String INTEGER = " INTEGER";
    private static final String BOOLEAN = " BOOLEAN";
    private static final String BLOB = " BLOB";

    private static final String CREATE_TABLE = "CREATE TABLE ";
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String UNIQUE = " UNIQUE";
    private static final String NOT_NULL = " NOT NULL";
    private static final String DEFAULT = " DEFAULT ";
    private static final String REFERENCES = " REFERENCES ";
    private static final String ON_DELETE = " ON DELETE";
    private static final String ON_CONFLICT = " ON CONFLICT";
    private static final String CASCADE = " CASCADE";
    private static final String IGNORE = " IGNORE";
    private static final String FAIL = " FAIL";

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_LOG = CREATE_TABLE + DbContract.Log.TABLE + " (" +
            DbContract.Log._ID + INTEGER + PRIMARY_KEY + COMMA_SEP +
            DbContract.Log.COLUMN_TIME + INTEGER + NOT_NULL + COMMA_SEP +
            DbContract.Log.COLUMN_TIME_END + INTEGER + COMMA_SEP +
            DbContract.Log.COLUMN_TITLE + TEXT + COMMA_SEP +
            DbContract.Log.COLUMN_CONTENT + TEXT + " )";

    /*
    @Deprecated
    private static final String SQL_CREATE_LOG_ATTACHMENT =
            CREATE_TABLE + DbContract.LogAttachment.TABLE + "(" +
                    DbContract.LogAttachment._ID + INTEGER + PRIMARY_KEY + COMMA_SEP +
                    DbContract.LogAttachment.COLUMN_LOG + INTEGER + NOT_NULL + REFERENCES +
                            DbContract.Log.TABLE + "(" + DbContract.Log._ID + ")" +
                                    ON_DELETE + CASCADE + COMMA_SEP +
                    DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME + TEXT + COMMA_SEP +
                    DbContract.LogAttachment.COLUMN_ATTACHMENT_TYPE + TEXT + COMMA_SEP +
                    DbContract.LogAttachment.COLUMN_ATTACHMENT_DATA + BLOB + COMMA_SEP +
                    UNIQUE + " (" + DbContract.LogAttachment.COLUMN_LOG + COMMA_SEP +
                            DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME + ")" +
                                    ON_CONFLICT + FAIL + ")";
    */

    private static final String SQL_CREATE_LOG_ATTACHMENT2 =
            CREATE_TABLE + DbContract.LogAttachment2.TABLE + "(" +
                    DbContract.LogAttachment2._ID + INTEGER + PRIMARY_KEY + COMMA_SEP +
                    DbContract.LogAttachment2.COLUMN_LOG + INTEGER + NOT_NULL + REFERENCES +
                            DbContract.Log.TABLE + "(" + DbContract.Log._ID + ")" +
                                    ON_DELETE + CASCADE + COMMA_SEP +
                    DbContract.LogAttachment2.COLUMN_ATTACHMENT_NAME + TEXT + COMMA_SEP +
                    DbContract.LogAttachment2.COLUMN_ATTACHMENT_TYPE + TEXT + COMMA_SEP +
                    UNIQUE + " (" + DbContract.LogAttachment2.COLUMN_LOG + COMMA_SEP +
                            DbContract.LogAttachment2.COLUMN_ATTACHMENT_NAME + ")" +
                                    ON_CONFLICT + FAIL + ")";

    private static final String SQL_CREATE_TAG =
            CREATE_TABLE + DbContract.Tag.TABLE + " (" +
                    DbContract.Tag._ID + INTEGER + PRIMARY_KEY + COMMA_SEP +
                    DbContract.Tag.COLUMN_NAME + TEXT + COMMA_SEP +
                    DbContract.Tag.COLUMN_DESCRIPTION + TEXT + " )";

    private static final String SQL_CREATE_LOG_TAGS =
            CREATE_TABLE + DbContract.LogTags.TABLE + " (" +
                    DbContract.LogTags.COLUMN_LOG + INTEGER + REFERENCES +
                            DbContract.Log.TABLE + "(" + DbContract.Log._ID + ")" +
                                    ON_DELETE + CASCADE + COMMA_SEP +
                    DbContract.LogTags.COLUMN_TAG + INTEGER + REFERENCES +
                            DbContract.Tag.TABLE + "(" + DbContract.Tag._ID + ")" +
                                    ON_DELETE + CASCADE + COMMA_SEP +
                    UNIQUE + " (" + DbContract.LogTags.COLUMN_LOG + COMMA_SEP +
                            DbContract.LogTags.COLUMN_TAG + ")" + ON_CONFLICT + IGNORE + ")";

    private static final String SQL_CREATE_LOG_FILTER =
            CREATE_TABLE + DbContract.LogFilter.TABLE + " (" +
                    DbContract.LogFilter._ID + INTEGER + PRIMARY_KEY + COMMA_SEP +
                    DbContract.LogFilter.COLUMN_NAME + TEXT + COMMA_SEP +
                    DbContract.LogFilter.COLUMN_SORT_ORDER + TEXT + COMMA_SEP +
                    DbContract.LogFilter.COLUMN_STRICT_FILTER_TAGS + BOOLEAN + ")";

    private static final String SQL_CREATE_LOG_FILTER_TAGS =
            CREATE_TABLE + DbContract.LogFilter_Tags.TABLE + " (" +
                    DbContract.LogFilter_Tags.COLUMN_FILTER + INTEGER + REFERENCES +
                            DbContract.LogFilter.TABLE + "(" + DbContract.LogFilter._ID + ")" +
                                    ON_DELETE + CASCADE + COMMA_SEP +
                    DbContract.LogFilter_Tags.COLUMN_TAG + INTEGER + REFERENCES +
                            DbContract.Tag.TABLE + "(" + DbContract.Tag._ID + ")" +
                                    ON_DELETE + CASCADE + COMMA_SEP +
                    DbContract.LogFilter_Tags.COLUMN_EXCLUDE_TAG + BOOLEAN + NOT_NULL +
                            DEFAULT + "FALSE" + COMMA_SEP +
                    UNIQUE + " (" + DbContract.LogFilter_Tags.COLUMN_FILTER + COMMA_SEP +
                            DbContract.LogFilter_Tags.COLUMN_TAG + ")" + ON_CONFLICT + IGNORE + ")";

    private Context mContext;

    public static void init(Context context) {
        if (INITIALIZED) {
            return;
        }
        context = context.getApplicationContext();
        SQLiteDatabase.loadLibs(context);
        INITIALIZED = true;
    }

    public DbHelper(Context context) {
        super(context, NAME, null, VERSION);
        init(context);
        mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LOG);
        db.execSQL(SQL_CREATE_LOG_ATTACHMENT2);
        db.execSQL(SQL_CREATE_TAG);
        db.execSQL(SQL_CREATE_LOG_TAGS);
        db.execSQL(SQL_CREATE_LOG_FILTER);
        db.execSQL(SQL_CREATE_LOG_FILTER_TAGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        upgradeDb(db, oldVersion, newVersion);
   }

    static void upgradeDb(SQLiteDatabase db, int oldVersion, int newVersion)
            throws UnsupportedUpgradeException {
         switch (oldVersion) {
            case 1:
                // Add description to tags
                db.execSQL("alter table " + DbContract.Tag.TABLE + " add " +
                        DbContract.Tag.COLUMN_DESCRIPTION + TEXT);
            case 2:
                // Add new tables for log filters
                db.execSQL(SQL_CREATE_LOG_FILTER);
                db.execSQL(SQL_CREATE_LOG_FILTER_TAGS);
            case 3:
                // LogFilter_Tags: add exclude boolean
                db.execSQL("alter table " + DbContract.LogFilter_Tags.TABLE + " add " +
                        DbContract.LogFilter_Tags.COLUMN_EXCLUDE_TAG + BOOLEAN + NOT_NULL + DEFAULT + "FALSE");
            case 4:
                /* Internal release
                // Create Attachment table
                db.execSQL(SQL_CREATE_LOG_ATTACHMENT);
                */
            case 5:
                /* Internal release
                // Move to newer attachment implementation
                db.execSQL(DROP_TABLE + DbContract.LogAttachment.TABLE);
                */
                db.execSQL(SQL_CREATE_LOG_ATTACHMENT2);

                // Remember to keep break before default!
                break;
            default:
                Log.e(TAG, "unhandled upgrade from " + oldVersion + " to " + newVersion);
                throw new UnsupportedUpgradeException(oldVersion, newVersion);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign key on update/delete functionality
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    }

    public static class UnsupportedUpgradeException extends RuntimeException {
        public final int oldVersion;
        public final int newVersion;

        public UnsupportedUpgradeException(int oldVersion, int newVersion) {
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }
    }


    public static long saveLogItemToDb(LogItem logItem, SQLiteDatabase db, boolean add,
                                       boolean preserveTimestamp) {
        // Last modified timestamp
        if (!preserveTimestamp) {
            logItem.timeEnd = System.currentTimeMillis();
        }
        // Save to db
        ContentValues values = new ContentValues();
        values.put(DbContract.Log.COLUMN_TITLE, logItem.title);
        values.put(DbContract.Log.COLUMN_CONTENT, logItem.content);
        values.put(DbContract.Log.COLUMN_TIME, logItem.time);
        values.put(DbContract.Log.COLUMN_TIME_END, logItem.timeEnd);
        long id;
        if (logItem.id == LogItem.ID_NONE) {
            id = LogItem.generateId();
        } else {
            id = logItem.id;
        }
        if (add) {
            values.put(DbContract.Log._ID, id);
            id = db.insert(DbContract.Log.TABLE, "null", values);
        } else {
            String selection = DbContract.Log._ID + " = ?";
            String[] selectionArgs = {String.valueOf(id)};
            db.update(DbContract.Log.TABLE, values, selection, selectionArgs);
        }
        return id;
    }

    public static void removeLogItemsFromDb(Context context, SQLiteDatabase db,
                                            LogItem... logItems) {
        if (logItems.length == 0) {
            return;
        }
        String selection = DbContract.Log._ID + " = ?";
        String[] selectionArgs = new String[logItems.length];
        selectionArgs[0] = String.valueOf(logItems[0].id);
        for (int i = 1; i < logItems.length; i++) {
            selection += " OR " + DbContract.Log._ID + " = ?";
            selectionArgs[i] = String.valueOf(logItems[i].id);
        }
        db.delete(DbContract.Log.TABLE, selection, selectionArgs);
        // Remove attachments storage
        for (LogItem logItem: logItems) {
            FileHelper.removeAllAttachments(context, logItem);
        }
    }

    public static void saveLogTagsToDb(LogItem logItem, SQLiteDatabase db,
                                    @Nullable List<TagItem> add,
                                    @Nullable List<TagItem> remove) {
        for (TagItem tag: add) {
            ContentValues tagValues = new ContentValues();
            tagValues.put(DbContract.LogTags.COLUMN_LOG, logItem.id);
            tagValues.put(DbContract.LogTags.COLUMN_TAG, tag.id);
            db.insert(DbContract.LogTags.TABLE, "null", tagValues);
        }
        if (!remove.isEmpty()) {
            String selection = DbContract.LogTags.COLUMN_LOG + " = ? AND ("
                    + DbContract.LogTags.COLUMN_TAG + " = ?";
            String[] selectionArgs = new String[remove.size()+1];
            selectionArgs[0] = String.valueOf(logItem.id);
            selectionArgs[1] = String.valueOf(remove.get(0).id);
            for (int i = 1; i < remove.size(); i++) {
                selection += " OR " + DbContract.LogTags.COLUMN_TAG + " = ?";
                selectionArgs[i+1] = String.valueOf(remove.get(i).id);
            }
            selection += ")";
            db.delete(DbContract.LogTags.TABLE, selection, selectionArgs);
        }
    }

    public static void addLogAttachmentDbEntry(SQLiteDatabase db, LogItem.Attachment attachment) {
        if (attachment.id == LogItem.Attachment.ID_NONE) {
            attachment.id = LogItem.Attachment.generateId();
        }
        ContentValues values = new ContentValues();
        values.put(DbContract.LogAttachment2._ID, attachment.id);
        values.put(DbContract.LogAttachment2.COLUMN_LOG, attachment.logId);
        values.put(DbContract.LogAttachment2.COLUMN_ATTACHMENT_NAME, attachment.name);
        values.put(DbContract.LogAttachment2.COLUMN_ATTACHMENT_TYPE, attachment.type);
        db.insert(DbContract.LogAttachment2.TABLE, "null", values);
    }

    public static void renameLogAttachmentDbEntry(SQLiteDatabase db,
                                                  LogItem.Attachment attachment) {
        ContentValues values = new ContentValues();
        values.put(DbContract.LogAttachment2.COLUMN_ATTACHMENT_NAME, attachment.name);
        String selection = DbContract.LogAttachment2._ID + " = ?";
        String[] selectionArgs = {String.valueOf(attachment.id)};
        db.update(DbContract.LogAttachment2.TABLE, values, selection, selectionArgs);
    }

    public static void deleteLogAttachmentDbEntry(SQLiteDatabase db,
                                                  LogItem.Attachment attachment) {
        String selection = DbContract.LogAttachment2._ID + " = ?";
        db.delete(DbContract.LogAttachment2.TABLE, selection,
                new String[]{String.valueOf(attachment.id)});
    }

    public static void saveTagItem(SQLiteDatabase db, TagItem tagItem, boolean add) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Tag.COLUMN_NAME, tagItem.name);
        values.put(DbContract.Tag.COLUMN_DESCRIPTION, tagItem.description);
        if (tagItem.id == TagItem.ID_NONE) {
            tagItem.id = TagItem.generateId();
        }
        if (add) {
            values.put(DbContract.Tag._ID, tagItem.id);
            tagItem.id = db.insert(DbContract.Tag.TABLE, "null", values);
        } else {
            String selection = DbContract.Log._ID + " = ?";
            String[] selectionArgs = {String.valueOf(tagItem.id)};
            db.update(DbContract.Tag.TABLE, values, selection, selectionArgs);
        }
    }

    public static void deleteTagItem(SQLiteDatabase db, TagItem tagItem) {
        String selection = DbContract.Tag._ID + " = ?";
        String[] selectionArgs = {String.valueOf(tagItem.id)};
        db.delete(DbContract.Tag.TABLE, selection, selectionArgs);
    }
}
