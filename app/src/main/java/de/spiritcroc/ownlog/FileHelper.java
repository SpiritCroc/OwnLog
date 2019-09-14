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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.spiritcroc.ownlog.data.LogItem;

public abstract class FileHelper {

    private static final String TAG = FileHelper.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String ATTACHMENTS_PATH = "attachments";
    private static final String EXPORT_PATH = "export";
    private static final String FILE_PROVIDER_AUTHORITY = "de.spiritcroc.ownlog.fileprovider";

    private static final int FILE_BUFFER = 2048;


    /**
     * @param requestCode
     * The request code ID that should be checked for in onRequestPermissionsResult by the caller
     * @return
     * True if all permissions already granted, false if they have to get granted first
     */
    public static boolean checkFileUriReadPermissions(Activity activity, Uri uri, int requestCode) {
        boolean isStoragePermissionRequired = "file".equals(uri.getScheme());
        if (isStoragePermissionRequired && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
            return false;
        } else {
            return true;
        }
    }

    public static Uri getFileShare(Context context, File shareFile) {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile);
    }

    public static Uri getAttachmentFileShare(Context context, LogItem.Attachment attachment) {
        return getFileShare(context, getAttachmentFile(context, attachment));
    }

    public static File getAttachmentFile(Context context, LogItem.Attachment attachment) {
        return getAttachmentFile(context, attachment.logId, attachment.name);
    }

    public static File getAttachmentFile(Context context, long logId, String name) {
        return getAttachmentFile(getAttachmentsPathForLog(context, logId), name);
    }

    public static File getAttachmentFile(File baseDir, String name) {
        return new File(baseDir, name);
    }

    private static File getAttachmentsPathForLog(Context context, long logId) {
        File path = getAttachmentsPathForLog(getAttachmentsBasePath(context), logId);
        path.mkdirs();
        return path;
    }

    private static File getAttachmentsPathForLog(File baseDir, long logId) {
        return new File(baseDir, "" + logId);
    }

    private static File getAttachmentsBasePath(Context context) {
       File result = new File(context.getFilesDir(), ATTACHMENTS_PATH);
       result.mkdirs();
       return result;
    }

    public static void removeAllAttachments(Context context, LogItem logItem) {
        rmdir(getAttachmentsPathForLog(context, logItem.id));
    }


    private static File getExportPath(Context context) {
        return new File(context.getCacheDir(), EXPORT_PATH);
    }

    private static File getExportFile(Context context) {
        File path = new File(getExportPath(context), generateExportFileName(context));
        path.mkdirs();
        return path;
    }

    private static File getExportDbFile(Context context) {
        return new File(getExportPath(context), context.getString(R.string.export_file_db));
    }

    private static String getExportAttachmentsPath(Context context) {
        return context.getString(R.string.export_file_attachments);
    }

    private static String generateExportFileName(Context context) {
        return context.getString(R.string.export_file_name,
                new SimpleDateFormat(context.getString(R.string.export_file_date_format), Locale.US)
                        .format(Calendar.getInstance().getTime()));
    }

    public static File generateExport(Context context, SQLiteDatabase db) {
        BufferedInputStream in = null;
        ZipOutputStream out = null;
        try {
            File outFile = getExportFile(context);
            outFile.delete();
            outFile.createNewFile();
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            File dbExportFile = getExportDbFile(context);
            PasswdHelper.cloneDb(db, dbExportFile);
            addFileToZip(out, dbExportFile, dbExportFile.getName());
            // dbExportFile only temporary needed for zipping
            dbExportFile.delete();
            File attachmentsPath = getAttachmentsBasePath(context);
            // Structure of attachment directory: entry/attachment
            String exportAttachmentsPath = getExportAttachmentsPath(context);
            for (File entry : attachmentsPath.listFiles()) {
                for (File file : entry.listFiles()) {
                    addFileToZip(out, file, exportAttachmentsPath + File.separator
                            + entry.getName() + File.separator + file.getName());
                }
            }
            return outFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static ImportFiles unpackImport(Context context, InputStream inputStream) {
        File importingDir = new File(context.getCacheDir(),
                context.getString(R.string.import_file_dir));
        rmdir(importingDir);
        importingDir.mkdirs();
        ZipInputStream in = new ZipInputStream(inputStream);
        ZipEntry entry;
        try {
            while ((entry = in.getNextEntry()) != null) {
                String path = importingDir.getAbsolutePath() + File.separator + entry.getName();
                if (DEBUG) Log.d(TAG, "Unzipping path: " + path);
                File f = new File(path);
                if (entry.isDirectory()) {
                    if (!f.isDirectory()) {
                        f.mkdirs();
                    }
                } else {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                    FileOutputStream out = new FileOutputStream(path, false);
                    try {
                        int size;
                        while ((size = in.read()) != -1) {
                            out.write(size);
                        }
                        in.closeEntry();
                    } finally {
                        try {
                            out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Import failed", e);
            return null;
        }
        File logDbFile = new File(importingDir,
                context.getString(R.string.import_file_db));
        File attachmentsDir = new File(importingDir,
                context.getString(R.string.import_file_attachments));
        if (logDbFile.exists()) {
            return new ImportFiles(logDbFile, attachmentsDir);
        } else {
            Log.w(TAG, "Import failed: database not found");
            return null;
        }
    }

    public static boolean importAttachmentFile(Context context, LogItem.Attachment attachment,
                                               ImportFiles importFiles) {
        File source = getAttachmentFile(getAttachmentsPathForLog(importFiles.attachmentsDir,
                attachment.logId), attachment.name);
        File target = getAttachmentFile(context, attachment);
        if (!source.exists()) {
            Log.w(TAG, "importAttachmentFile: " + source.getAbsolutePath() +
                    " does not exist");
            return false;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
        }
        return true;
    }

    public static class ImportFiles {
        public final File logDbFile;
        public final File attachmentsDir;
        public ImportFiles(File logDbFile, File attachmentsDir) {
            this.logDbFile = logDbFile;
            this.attachmentsDir = attachmentsDir;
        }
    }

    private static void addFileToZip(ZipOutputStream zip, File file, String pathInZip)
            throws IOException {
        byte[] data = new byte[FILE_BUFFER];
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            ZipEntry entry = new ZipEntry(pathInZip);
            zip.putNextEntry(entry);
            int size;
            while ((size = in.read(data, 0, FILE_BUFFER)) != -1) {
                zip.write(data, 0, size);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

   public static void rmdir(File dir) {
        if (dir == null) return;
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                rmdir(file);
            }
        }
        dir.delete();
    }


    static void onExit(Context context) {
        rmdir(context.getCacheDir());
    }
}
