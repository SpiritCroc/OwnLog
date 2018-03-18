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

package de.spiritcroc.ownlog;

import android.content.Context;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.spiritcroc.ownlog.data.ImportItemInfo;

public final class Util {

    private static final int FILESIZE_KIB = 1024;
    private static final int FILESIZE_MIB = FILESIZE_KIB * 1024;
    private static final int FILESIZE_GIB = FILESIZE_MIB * 1024;

    // No instantiation wanted
    private Util() {}

    public static String formatFileSize(Context context, long size) {
        if (size > FILESIZE_GIB) {
            return context.getString(R.string.filesize_gib, size/FILESIZE_GIB);
        } else if (size > FILESIZE_MIB) {
            return context.getString(R.string.filesize_mib, size/FILESIZE_MIB);
        } else if (size > FILESIZE_KIB) {
            return context.getString(R.string.filesize_kib, size/FILESIZE_KIB);
        } else {
            return context.getString(R.string.filesize_b, size);
        }
    }

    /**
     *
     * @param origin
     * Will be considered as old state for the diff
     * @param modify
     * Will be considered as new state for the diff
     * @param addedItems
     * Will include all items that were added in modify in comparison to origin after execution
     * @param removedItems
     * Will include all items that were removed in modify in comparison to origin after execution
     * @return
     * True if content identical (order not taken into account)
     */
    public static <T> boolean checkListDiff(List<T> origin, List<T> modify,
                                            @Nullable List<T> addedItems,
                                            @Nullable List<T> removedItems) {
        if (addedItems == null) {
            addedItems = new ArrayList<>();
        } else {
            addedItems.clear();
        }
        if (removedItems == null) {
            removedItems = new ArrayList<>();
        } else {
            removedItems.clear();
        }

        for (T o: origin) {
            if (!modify.contains(o)) {
                removedItems.add(o);
            }
        }

        for (T m: modify) {
            if (!origin.contains(m)) {
                addedItems.add(m);
            }
        }

        return addedItems.isEmpty() && removedItems.isEmpty();
    }

    public static <T>ImportItemInfo.ChangeStatus getListDiffChangeStatus(List<T> origin,
                                                                           List<T> modify) {
        ArrayList<T> newItems = new ArrayList<>();
        ArrayList<T> oldItems = new ArrayList<>();
        checkListDiff(origin, modify, newItems, oldItems);
        if (newItems.isEmpty()) {
            if (oldItems.isEmpty()) {
                return ImportItemInfo.ChangeStatus.SAME;
            } else {
                return ImportItemInfo.ChangeStatus.DEPRECATED;
            }
        } else {
            if (oldItems.isEmpty()) {
                return ImportItemInfo.ChangeStatus.UPDATED;
            } else {
                return ImportItemInfo.ChangeStatus.CHANGED;
            }
        }
    }
}
