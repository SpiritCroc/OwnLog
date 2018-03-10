/*
 * Copyright (C) 2017 SpiritCroc
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


import android.content.Context;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = DbHelper.class.getSimpleName();

    private static boolean INITIALIZED = false;

    public static final int VERSION = 6;
    public static final String NAME = "log.db";
    public static final String NAME_COPY = "log2.db";
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
                Log.e(TAG, "unhandled upgrade from " + oldVersion + " to " +
                        newVersion + "; discarding content");
                createBackup(mContext);
                recreateDb(db);
                break;
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

    private void recreateDb(SQLiteDatabase db) {
        db.execSQL(DROP_TABLE + DbContract.Log.TABLE);
        db.execSQL(DROP_TABLE + DbContract.LogAttachment2.TABLE);
        db.execSQL(DROP_TABLE + DbContract.Tag.TABLE);
        db.execSQL(DROP_TABLE + DbContract.LogTags.TABLE);
        db.execSQL(DROP_TABLE + DbContract.LogFilter.TABLE);
        db.execSQL(DROP_TABLE + DbContract.LogFilter_Tags.TABLE);
        onCreate(db);
    }

    public static void createBackup(Context context) {
        deleteBackup(context);

        File dbFile = context.getDatabasePath(DbHelper.NAME);
        File dbBackupFile = context.getDatabasePath(DbHelper.NAME_BACKUP);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(dbFile);
            outputStream = new FileOutputStream(dbBackupFile, false);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteBackup(Context context) {
        context.getDatabasePath(DbHelper.NAME_BACKUP).delete();
    }
}
