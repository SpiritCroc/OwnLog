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

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;

import de.spiritcroc.ownlog.data.LogItem;

public abstract class FileHelper {

    private static final String ATTACHMENTS_PATH = "attachments";
    private static final String FILE_PROVIDER_AUTHORITY = "de.spiritcroc.ownlog.fileprovider";

    public static Uri getShareFile(Context context, LogItem.Attachment attachment) {
        File shareFile = getAttachmentFile(context, attachment);
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile);
    }

    public static File getAttachmentFile(Context context, LogItem.Attachment attachment) {
        return getAttachmentFile(context, attachment.logId, attachment.name);
    }

    public static File getAttachmentFile(Context context, long logId, String name) {
        File path = new File(context.getFilesDir(), ATTACHMENTS_PATH);
        File logPath = new File(path, "" + logId);
        logPath.mkdirs();
        return new File(logPath, name);
    }
}
