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

import android.os.AsyncTask;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

public class LoadLogItemAttachmentsTask
        extends AsyncTask<Long, Void, ArrayList<LogItem.Attachment>> {

    protected SQLiteDatabase mDb;

    public LoadLogItemAttachmentsTask(SQLiteDatabase db) {
        mDb = db;
    }

    @Override
    protected ArrayList<LogItem.Attachment> doInBackground(Long... params) {
        ArrayList<LogItem.Attachment> result = loadAttachments(mDb, getSelection(params),
                getProjection(), getSortOrder());
        mDb.close();
        return result;
    }

    protected String getSelection(Long... ids) {
        if (ids.length == 0) {
            // Load all attachments
            return null;
        } else {
            String selection = DbContract.LogAttachment.COLUMN_LOG + " = " + ids[0];
            for (int i = 1; i < ids.length; i++) {
                selection += " OR " + DbContract.LogAttachment.COLUMN_LOG + " = " + ids[1];
            }
            return selection;
        }
    }

    protected String[] getProjection() {
        return new String[] {
                DbContract.LogAttachment._ID,
                DbContract.LogAttachment.COLUMN_LOG,
                DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME,
                DbContract.LogAttachment.COLUMN_ATTACHMENT_TYPE,
                DbContract.LogAttachment.COLUMN_ATTACHMENT_DATA,
        };
    }

    public static String getSortOrder() {
        return  DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME + " ASC";
    }

    /**
     * Does not close db, do so from calling method!
     */
    public static ArrayList<LogItem.Attachment> loadAttachments(SQLiteDatabase db, String selection,
                                                                String[] projection,
                                                                String sortOrder) {
        Cursor cursor = db.query(DbContract.LogAttachment.TABLE, projection,
                selection, null, null, null, sortOrder);
        ArrayList<LogItem.Attachment> result = new ArrayList<>();
        if (!cursor.moveToFirst()) {
            cursor.close();
            return result;
        }
        int indexId = cursor.getColumnIndex(DbContract.LogAttachment._ID);
        int indexLogId = cursor.getColumnIndex(DbContract.LogAttachment.COLUMN_LOG);
        int indexName = cursor.getColumnIndex(DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME);
        int indexType = cursor.getColumnIndex(DbContract.LogAttachment.COLUMN_ATTACHMENT_TYPE);
        int indexData = cursor.getColumnIndex(DbContract.LogAttachment.COLUMN_ATTACHMENT_DATA);
        do {
            LogItem.Attachment item = new LogItem.Attachment();
            if (indexId >= 0) {
                item.id = cursor.getLong(indexId);
            }
            if (indexLogId >= 0) {
                item.logId = cursor.getLong(indexLogId);
            }
            if (indexName >= 0) {
                item.name = cursor.getString(indexName);
            }
            if (indexType >= 0) {
                item.type = cursor.getString(indexType);
            }
            if (indexData >= 0) {
                item.data = cursor.getBlob(indexData);
            }
            result.add(item);
        } while (cursor.moveToNext());
        cursor.close();
        return result;
    }

    /**
     * Does not close db, do so from calling method!
     */
    public static boolean hasAttachments(SQLiteDatabase db, long logId) {
        Cursor cursor = db.query(DbContract.LogAttachment.TABLE,
                new String[]{DbContract.LogAttachment._ID},
                DbContract.LogAttachment.COLUMN_LOG + " = " + logId,
                null, null, null, null
        );
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }
}
