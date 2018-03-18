/*
 * Copyright (C) 2018 SpiritCroc
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

import android.content.res.Resources;

import java.util.ArrayList;

import de.spiritcroc.ownlog.data.TagItem;

public class TagFormatter {

    public static String formatTags(Resources resources, ArrayList<TagItem> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        String result = tags.get(0).name;
        for (int i = 1; i < tags.size(); i++) {
            result += resources.getString(R.string.log_list_tag_list_separator) + tags.get(1).name;
        }
        return result;
    }

}
