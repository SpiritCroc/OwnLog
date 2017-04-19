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

import java.util.ArrayList;

import de.spiritcroc.ownlog.DateFormatter;

public class LogItem implements Parcelable, DateFormatter.DateProvider {
    public long id = -1;
    public long time = -1;
    public long timeEnd = -1;
    public String title = "";
    public String content = "";
    public ArrayList<TagItem> tags;

    public LogItem(long id) {
        this.id = id;
    }

    public LogItem(long id, long time, long timeEnd, String title, String content) {
        this.id = id;
        this.time = time;
        this.timeEnd = timeEnd;
        this.title = title;
        this.content = content;
    }

    @Override
    public long getDate() {
        return time;
    }

    public static long generateId() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + id + ";" + time + ";" + timeEnd + ";" + title
                + ";" + content + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof LogItem) {
            LogItem compare = (LogItem) o;
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
        out.writeLongArray(new long[]{id, time, timeEnd});
        out.writeStringArray(new String[]{title, content});
        if (tags != null) {
            out.writeTypedArray(tags.toArray(new TagItem[tags.size()]), 0);
        }
    }

    public static final Parcelable.Creator<LogItem> CREATOR
            = new Parcelable.Creator<LogItem>() {
        @Override
        public LogItem createFromParcel(Parcel in) {
            long[] longs = in.createLongArray();
            String[] strings = in.createStringArray();
            LogItem result = new LogItem(longs[0], longs[1], longs[2], strings[0], strings[1]);
            result.tags = in.createTypedArrayList(TagItem.CREATOR);
            return result;
        }

        @Override
        public LogItem[] newArray(int size) {
            return new LogItem[size];
        }
    };
}
