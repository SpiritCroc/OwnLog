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

public abstract class LoadTagItemsTask extends AsyncTask<Void, Void, ArrayList<TagItem>>  {

    protected SQLiteDatabase mDb;

    public LoadTagItemsTask(SQLiteDatabase db) {
        mDb = db;
    }

    @Override
    protected ArrayList<TagItem> doInBackground(Void... params) {
        ArrayList<TagItem> result = loadAvailableTags(mDb, getSelection());
        mDb.close();
        return result;
    }

    protected String getSelection() {
        return null;
    }

    public static ArrayList<TagItem> loadAvailableTags(SQLiteDatabase db, String selection) {
        String[] projection = new String[] {
                DbContract.Tag._ID,
                DbContract.Tag.COLUMN_NAME,
                DbContract.Tag.COLUMN_DESCRIPTION,
        };
        Cursor cursor = db.query(DbContract.Tag.TABLE, projection, selection, null, null, null,
                TagItem.getSortOrder());
        ArrayList<TagItem> result = new ArrayList<>();
        if (!cursor.moveToFirst()) {
            cursor.close();
            // Don't close db, we still need it
            return result;
        }
        int indexId = cursor.getColumnIndex(DbContract.Tag._ID);
        int indexName = cursor.getColumnIndex(DbContract.Tag.COLUMN_NAME);
        int indexDescription = cursor.getColumnIndex(DbContract.Tag.COLUMN_DESCRIPTION);
        do {
            TagItem item = new TagItem(-1);
            if (indexId >= 0) {
                item.id = cursor.getLong(indexId);
            }
            if (indexName >= 0) {
                item.name = cursor.getString(indexName);
            }
            if (indexDescription >= 0) {
                item.description = cursor.getString(indexDescription);
            }
            result.add(item);
        } while (cursor.moveToNext());
        cursor.close();
        // Don't close db, we still need it
        return result;
    }
}
