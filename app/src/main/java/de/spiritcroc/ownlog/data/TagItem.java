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

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import java.util.List;

import de.spiritcroc.ownlog.Util;

public class TagItem implements Parcelable {

    public static final long ID_NONE = -1;

    public long id = ID_NONE;
    public String name;
    public String description;

    public TagItem(long id) {
        this.id = id;
    }

    public TagItem(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public TagItem(long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /**
     * See Util.checkListDiff
     */
    public static boolean checkTagListDiff(List<TagItem> origin, List<TagItem> modify,
                                           @Nullable List<TagItem> addedItems,
                                           @Nullable List<TagItem> removedItems) {
        return Util.checkListDiff(origin, modify, addedItems, removedItems);
    }

    public static String getSortOrder() {
        return DbContract.Tag.COLUMN_NAME + " ASC";
    }

    public static long generateId() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + id + ";" + name + ";" + description + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof TagItem) {
            TagItem compare = (TagItem) o;
            return id != -1 && id == compare.id;
        } else {
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeStringArray(new String[]{name, description});
    }

    public static final Parcelable.Creator<TagItem> CREATOR
            = new Parcelable.Creator<TagItem>() {
        @Override
        public TagItem createFromParcel(Parcel in) {
            long id = in.readLong();
            String[] strings = in.createStringArray();
            return new TagItem(id, strings[0], strings[1]);
        }

        @Override
        public TagItem[] newArray(int size) {
            return new TagItem[size];
        }
    };
}
