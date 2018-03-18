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

import net.sqlcipher.database.SQLiteDatabase;

public class ExternalDbReadHelper {

    private String mDbPath;
    private SQLiteDatabase mDb;

    public ExternalDbReadHelper(String path) {
        mDbPath = path;
    }

    public SQLiteDatabase getReadableDatabase(String password)
            throws DbHelper.UnsupportedUpgradeException {
        if (mDb == null) {
            mDb = SQLiteDatabase.openDatabase(mDbPath, password, null, SQLiteDatabase.OPEN_READONLY);
            // Make sure database is up-to-date with current db format
            // (or throw error is not upgradable)
            if (mDb.getVersion() != DbHelper.VERSION) {
                DbHelper.upgradeDb(mDb, mDb.getVersion(), DbHelper.VERSION);
            }
        }
        return mDb;
    }

    public void close() {
        mDb.close();
        mDb = null;
    }
}
