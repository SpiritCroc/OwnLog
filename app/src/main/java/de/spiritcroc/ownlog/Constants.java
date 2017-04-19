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

package de.spiritcroc.ownlog;

public class Constants {

    public static final String EXTRA_FRAGMENT_CLASS = "de.spiritcroc.ownlog.fragment_class";

    public static final String EXTRA_TITLE = "de.spiritcroc.ownlog.title";

    public static final String EXTRA_LOG_ITEM_ID = "de.spiritcroc.ownlog.extra.log_item_id";

    public static final String EXTRA_LOG_FILTER_ITEM_ID =
            "de.spiritcroc.ownlog.extra.log_filter_item_id";

    public static final String EXTRA_TAG_ITEM = "de.spiritcroc.ownlog.extra.tag_item";

    public static final String EXTRA_TAG_ACTION = "de.spiritcroc.ownlog.extra.tag_action";

    public static final int TAG_ACTION_ADD = 1;
    public static final int TAG_ACTION_EDIT = 2;
    public static final int TAG_ACTION_DELETE = 3;

    /**
     * Bundle that will be passed from the instantiated activity to its fragment
     */
    public static final String EXTRA_FRAGMENT_BUNDLE = "de.spiritcroc.ownlog.fragment_bundle";

    /**
     * Event which means the log list should be updated
     */
    public static final String EVENT_LOG_UPDATE = "de.spiritcroc.ownlog.event.log_update";

    /**
     * Event which means a tag has been added/edited/removed, so fragments editing items with tag
     * lists should update
     */
    public static final String EVENT_TAG_UPDATE = "de.spiritcroc.ownlog.event.tag_update";
}
