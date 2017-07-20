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

import android.content.Context;

public final class Util {

    private static final int FILESIZE_KIB = 1024;
    private static final int FILESIZE_MIB = FILESIZE_KIB * 1024;
    private static final int FILESIZE_GIB = FILESIZE_MIB * 1024;

    // No instantiation wanted
    private Util() {}

    public static String formatFileSize(Context context, int size) {
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
}
