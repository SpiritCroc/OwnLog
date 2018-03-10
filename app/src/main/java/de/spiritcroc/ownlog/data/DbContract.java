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

import android.provider.BaseColumns;

public abstract class DbContract {

    public static abstract class Log implements BaseColumns {
        public static final String TABLE = "log";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_TIME_END = "time_end";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CONTENT = "content";
    }

    /*
    @Deprecated
    public static abstract class LogAttachment implements BaseColumns {
        public static final String TABLE = "log_blobs";
        public static final String COLUMN_LOG = "log_id";
        public static final String COLUMN_ATTACHMENT_NAME = "attachment_name";
        public static final String COLUMN_ATTACHMENT_TYPE = "attachment_type";
        public static final String COLUMN_ATTACHMENT_DATA = "attachment_data";
    }
    */

    public static abstract class LogAttachment2 implements BaseColumns {
        public static final String TABLE = "log_files";
        public static final String COLUMN_LOG = "log_id";
        public static final String COLUMN_ATTACHMENT_NAME = "attachment_name";
        public static final String COLUMN_ATTACHMENT_TYPE = "attachment_type";
    }

    public static abstract class Tag implements BaseColumns {
        public static final String TABLE = "tag";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_DESCRIPTION = "description";
    }

    public static abstract class LogTags /*not implements BaseColumns*/ {
        public static final String TABLE = "log_tags";
        public static final String COLUMN_LOG = "log_id";
        public static final String COLUMN_TAG = "tag_id";
    }

    public static abstract class LogFilter implements BaseColumns {
        public static final String TABLE = "log_filter";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_SORT_ORDER = "sort_order";
        public static final String COLUMN_STRICT_FILTER_TAGS = "filter_tags";
    }

    public static abstract class LogFilter_Tags /*not implements BaseColumns*/ {
        public static final String TABLE = "log_filter_tags";
        public static final String COLUMN_FILTER = "filter_id";
        public static final String COLUMN_TAG = "tag_id";
        public static final String COLUMN_EXCLUDE_TAG = "exclude_tag";
    }
}
