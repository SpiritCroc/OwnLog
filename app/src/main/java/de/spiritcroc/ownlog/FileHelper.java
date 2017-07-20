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
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;

import de.spiritcroc.ownlog.data.LogItem;

public abstract class FileHelper {

    private static final String FILE_PROVIDER_AUTHORITY = "de.spiritcroc.ownlog.fileprovider";
    private static final String ATTACHMENTS_FILE_PROVIDER_PATH = "attachment_shares";

    /**
     * Saves the attachment's data in a file accessible by our file provider, and returns the Uri.
     * Overwrite the AsyncTask's onPreExecute to show UI feedback, and onPostExecute to use the file
     * or show an error message if the result is null.
     */
    public abstract static class ShareFileTask extends AsyncTask<LogItem.Attachment, Void, Uri> {

        protected abstract Context getContext();

        @Override
        protected Uri doInBackground(LogItem.Attachment... attachments) {
            if (attachments.length != 1) {
                throw new InvalidParameterException(
                        "Only exactly one attachment share at the time allowed");
            }
            return createShareFile(getContext(), attachments[0]);
        }
    }

    /**
     * Saves the attachment's data in a file accessible by our file provider.
     * @return
     * The URI of the file to share, or null on failure.
     */
    private static Uri createShareFile(Context context, LogItem.Attachment attachment) {
        File shareFile = getShareFile(context, attachment);
        if (saveBytesToFile(attachment.data, shareFile)) {
            return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile);
        } else {
            return null;
        }
    }

    private static File getShareFile(Context context, LogItem.Attachment attachment) {
        File sharePath = new File(context.getCacheDir(), ATTACHMENTS_FILE_PROVIDER_PATH);
        sharePath.mkdirs();
        return new File(sharePath, attachment.name);
    }

    private static boolean saveBytesToFile(byte[] data, File file) {
        BufferedOutputStream out = null;
        try {
            if (!file.exists() && !file.createNewFile()) {
                return false;
            }
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(data);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
        }
    }
}
