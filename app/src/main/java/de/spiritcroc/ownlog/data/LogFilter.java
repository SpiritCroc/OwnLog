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
import android.text.TextUtils;

import java.util.ArrayList;

import de.spiritcroc.ownlog.R;

/**
 * Helper to get SQL selection + order by clauses when requesting a Log list
 */

public class LogFilter {

    static final String DEFAULT_SORT_ORDER = DbContract.Log.COLUMN_TIME + " DESC";

    public long id;
    public String name;
    public String sortOrder = DEFAULT_SORT_ORDER;
    public boolean strictFilterTags = false;
    public ArrayList<TagItem> filterTagsList = new ArrayList<>();

    public LogFilter(long id) {
        this.id = id;
    }

    public String getSelection(String search) {
        return getSelection(DbContract.Log._ID, search);
    }

    public String getSelection(String logId, String search) {
        String result = null;
        // ArrayList with inital capacity of 2, since that's the current maximum
        // of possible restrictions
        ArrayList<String> restrictions = new ArrayList<>(2);
        if (!TextUtils.isEmpty(search)) {
            String[] searchArray = search.split(" ");
            for (String searchItem : searchArray) {
                String searchRestriction = "(" + DbContract.Log.COLUMN_TITLE + " LIKE '%"
                        + searchItem + "%') OR (" + DbContract.Log.COLUMN_CONTENT + " LIKE '%"
                        + searchItem + "%')";
                restrictions.add(searchRestriction);
            }
        }
        if (strictFilterTags) {
            if (filterTagsList.isEmpty()) {
                restrictions.add("NOT EXISTS (SELECT " + DbContract.LogTags.COLUMN_LOG
                        + " FROM " + DbContract.LogTags.TABLE
                        + " WHERE " + DbContract.LogTags.COLUMN_LOG + " = " + logId + ")");
            } else {
                for (TagItem tag: filterTagsList) {
                    restrictions.add("EXISTS (SELECT " + DbContract.LogTags.COLUMN_LOG
                            + " FROM " + DbContract.LogTags.TABLE
                            + " WHERE " + DbContract.LogTags.COLUMN_LOG + " = " + logId
                            +  " AND " + DbContract.LogTags.COLUMN_TAG + " = " + tag.id + ")");
                }
            }
        } else if (!filterTagsList.isEmpty()) {
            String restriction = "EXISTS (SELECT " + DbContract.LogTags.COLUMN_LOG
                    + " FROM " + DbContract.LogTags.TABLE
                    + " WHERE " + DbContract.LogTags.COLUMN_LOG + " = " + logId
                    + " AND (" + DbContract.LogTags.COLUMN_TAG
                            + " = " + filterTagsList.get(0).id;
            for (int i = 1; i < filterTagsList.size(); i++) {
                restriction += " OR " + DbContract.LogTags.COLUMN_TAG + " = "
                        + filterTagsList.get(i).id;
            }
            restriction += "))";
            restrictions.add(restriction);
        }
        if (!restrictions.isEmpty()) {
            result = "(" + restrictions.get(0) + ")";
            for (int i = 1; i < restrictions.size(); i++) {
                result += " AND (" + restrictions.get(i) + ")";
            }
        }
        return result;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public static long generateId() {
        return System.currentTimeMillis();
    }

    public static LogFilter getDefaultFilter(Context context) {
        LogFilter lf = new LogFilter(-1);
        lf.name = context.getString(R.string.log_filter_default_name);
        return lf;
    }

    public boolean isDefaultFilter() {
        return id == -1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof LogFilter) {
            return ((LogFilter) o).id == id;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + id + ";" + name + ";" + sortOrder + ")";
    }
}
