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
import android.os.AsyncTask;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

public abstract class LoadLogFiltersTask extends AsyncTask<Void, Void, ArrayList<LogFilter>> {

    private static final String DEFAULT_SORT_ORDER = DbContract.LogFilter.COLUMN_NAME + " ASC";

    protected SQLiteDatabase mDb;
    protected Context mContext;

    public LoadLogFiltersTask(SQLiteDatabase db, Context context) {
        mDb = db;
        mContext = context;
    }

    @Override
    protected ArrayList<LogFilter> doInBackground(Void... params) {
        ArrayList<LogFilter> result = getLogFilters(mDb, mContext, getSelection(), getProjection(),
                getSortOrder());
        mDb.close();
        return result;
    }

    public static ArrayList<LogFilter> getLogFilters(SQLiteDatabase db, Context context) {
        String selection = null;
        String[] projection = new String[] {
                DbContract.LogFilter._ID,
                DbContract.LogFilter.COLUMN_NAME,
                DbContract.LogFilter.COLUMN_SORT_ORDER,
                DbContract.LogFilter.COLUMN_STRICT_FILTER_TAGS,
        };
        return getLogFilters(db, context, selection, projection, DEFAULT_SORT_ORDER);
    }

    public static ArrayList<LogFilter> getLogFilters(SQLiteDatabase db, Context context,
                                                     String selection, String[] projection,
                                                     String sortOrder) {
        Cursor cursor = db.query(DbContract.LogFilter.TABLE, projection, selection, null, null,
                null, sortOrder);
        ArrayList<LogFilter> result = new ArrayList<>();
        // Always include default filter when querying all filters
        if (selection == null) {
            result.add(LogFilter.getDefaultFilter(context));
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return result;
        }
        int indexId = cursor.getColumnIndex(DbContract.LogFilter._ID);
        int indexName = cursor.getColumnIndex(DbContract.LogFilter.COLUMN_NAME);
        int indexSortOrder = cursor.getColumnIndex(DbContract.LogFilter.COLUMN_SORT_ORDER);
        int indexStrictFilterTags =
                cursor.getColumnIndex(DbContract.LogFilter.COLUMN_STRICT_FILTER_TAGS);
        do {
            LogFilter item = new LogFilter(-1);
            if (indexId >= 0) {
                item.id = cursor.getLong(indexId);
            }
            if (indexName >= 0) {
                item.name = cursor.getString(indexName);
            }
            if (indexSortOrder >= 0) {
                item.sortOrder = cursor.getString(indexSortOrder);
            }
            if (indexStrictFilterTags >= 0) {
                item.strictFilterTags = cursor.getInt(indexStrictFilterTags) != 0;
            }
            // filter tags
            String tagTable =
                    DbContract.LogFilter_Tags.TABLE + " AS lt JOIN "
                            + DbContract.Tag.TABLE + " AS t ON lt."
                            + DbContract.LogFilter_Tags.COLUMN_TAG
                            + " = t." + DbContract.Tag._ID;
            String[] tagProjection = new String[] {
                    "t." + DbContract.Tag._ID + " AS mId",
                    "t." + DbContract.Tag.COLUMN_NAME + " AS mName",
                    "t." + DbContract.Tag.COLUMN_DESCRIPTION + " AS mDescription",
                    "lt." + DbContract.LogFilter_Tags.COLUMN_EXCLUDE_TAG + " AS mExcludeTag"
            };
            String tagSelection = "lt." + DbContract.LogFilter_Tags.COLUMN_FILTER + " = " + item.id;
            Cursor tagCursor = db.query(tagTable, tagProjection, tagSelection, null, null,
                    null, TagItem.getSortOrder());
            int tagIndexId = tagCursor.getColumnIndex("mId");
            int tagIndexName = tagCursor.getColumnIndex("mName");
            int tagIndexDescription = tagCursor.getColumnIndex("mDescription");
            int tagIndexExcludeTag = tagCursor.getColumnIndex("mExcludeTag");
            item.filterTagsList.clear();
            item.filterExcludedTagsList.clear();
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
                    boolean excluded = tagIndexExcludeTag >= 0 &&
                            tagCursor.getInt(tagIndexExcludeTag) != 0;
                    if (excluded) {
                        item.filterExcludedTagsList.add(tagItem);
                    } else {
                        item.filterTagsList.add(tagItem);
                    }
                } while (tagCursor.moveToNext());
            }
            tagCursor.close();
            result.add(item);
        } while (cursor.moveToNext());
        cursor.close();
        return result;
    }

    protected String getSelection() {
        return null;
    }

    protected String[] getProjection() {
        return new String[] {
                DbContract.LogFilter._ID,
                DbContract.LogFilter.COLUMN_NAME,
                DbContract.LogFilter.COLUMN_SORT_ORDER,
                DbContract.LogFilter.COLUMN_STRICT_FILTER_TAGS,
        };
    }

    protected String getSortOrder() {
        return DEFAULT_SORT_ORDER;
    }
}
