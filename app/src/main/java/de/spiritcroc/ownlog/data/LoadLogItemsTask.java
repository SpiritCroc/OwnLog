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

public abstract class LoadLogItemsTask extends AsyncTask<Void, Void, ArrayList<LogItem>>  {

    protected SQLiteDatabase mDb;

    public LoadLogItemsTask(SQLiteDatabase db) {
        mDb = db;
    }

    @Override
    protected ArrayList<LogItem> doInBackground(Void... params) {
        ArrayList<LogItem> result = loadLogItems(mDb, getSelection(), getProjection(),
                getSortOrder(), shouldLoadTags(), shouldCheckAttachments());
        if (doInBackgroundClosesDb()) {
            mDb.close();
        }
        return result;
    }

    protected String getSelection() {
        return null;
    }

    protected String[] getProjection() {
        return getFullProjection();
    }

    public static String[] getFullProjection() {
        return new String[] {
                DbContract.Log._ID,
                DbContract.Log.COLUMN_TIME,
                DbContract.Log.COLUMN_TIME_END,
                DbContract.Log.COLUMN_TITLE,
                DbContract.Log.COLUMN_CONTENT,
        };
    }

    protected String getSortOrder() {
        return  LogFilter.DEFAULT_SORT_ORDER;
    }

    protected boolean shouldLoadTags() {
        return true;
    }

    protected boolean shouldCheckAttachments() {
        return false;
    }

    protected boolean doInBackgroundClosesDb() {
        return true;
    }

    public static ArrayList<LogItem> loadLogItems(SQLiteDatabase db, String selection,
                                                  String[] projection, String sortOrder,
                                                  boolean shouldLoadTags,
                                                  boolean shouldCheckAttachments) {
        Cursor cursor = db.query(DbContract.Log.TABLE, projection, selection, null, null,
                null, sortOrder);
        ArrayList<LogItem> result = new ArrayList<>();
        if (!cursor.moveToFirst()) {
            cursor.close();
            return result;
        }
        int indexId = cursor.getColumnIndex(DbContract.Log._ID);
        int indexTime = cursor.getColumnIndex(DbContract.Log.COLUMN_TIME);
        int indexTimeEnd = cursor.getColumnIndex(DbContract.Log.COLUMN_TIME_END);
        int indexTitle = cursor.getColumnIndex(DbContract.Log.COLUMN_TITLE);
        int indexContent = cursor.getColumnIndex(DbContract.Log.COLUMN_CONTENT);
        do {
            LogItem item = new LogItem(-1);
            if (indexId >= 0) {
                item.id = cursor.getLong(indexId);
            }
            if (indexTime >= 0) {
                item.time = cursor.getLong(indexTime);
            }
            if (indexTimeEnd >= 0) {
                item.timeEnd = cursor.getLong(indexTimeEnd);
            }
            if (indexTitle >= 0) {
                item.title = cursor.getString(indexTitle);
            }
            if (indexContent >= 0) {
                item.content = cursor.getString(indexContent);
            }
            if (shouldLoadTags) {
                String tagTable =
                        DbContract.LogTags.TABLE + " AS lt JOIN "
                                + DbContract.Tag.TABLE + " AS t ON lt."
                                + DbContract.LogTags.COLUMN_TAG + " = t." + DbContract.Tag._ID;
                String[] tagProjection = new String[] {
                        "t." + DbContract.Tag._ID + " AS mId",
                        "t." + DbContract.Tag.COLUMN_NAME + " AS mName",
                        "t." + DbContract.Tag.COLUMN_DESCRIPTION + " AS mDescription",
                };
                String tagSelection = "lt." + DbContract.LogTags.COLUMN_LOG + " = " + item.id;
                Cursor tagCursor = db.query(tagTable, tagProjection, tagSelection, null, null,
                        null, TagItem.getSortOrder());
                int tagIndexId = tagCursor.getColumnIndex("mId");
                int tagIndexName = tagCursor.getColumnIndex("mName");
                int tagIndexDescription = tagCursor.getColumnIndex("mDescription");
                item.tags = new ArrayList<>();
                if (tagCursor.moveToFirst()) {
                    do {
                        TagItem  tagItem = new TagItem(-1);
                        if (tagIndexId >= 0) {
                            tagItem.id = tagCursor.getLong(tagIndexId);
                        }
                        if (tagIndexName >= 0) {
                            tagItem.name = tagCursor.getString(tagIndexName);
                        }
                        if (tagIndexDescription >= 0) {
                            tagItem.description = tagCursor.getString(tagIndexDescription);
                        }
                        item.tags.add(tagItem);
                    } while (tagCursor.moveToNext());
                }
                tagCursor.close();
            }
            if (shouldCheckAttachments) {
                item.hasAttachments = LoadLogItemAttachmentsTask.hasAttachments(db, item.id);
            }
            result.add(item);
        } while (cursor.moveToNext());
        cursor.close();
        return result;
    }
}
