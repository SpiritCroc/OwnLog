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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import java.io.File;

import de.spiritcroc.ownlog.data.DbHelper;
import de.spiritcroc.ownlog.ui.fragment.RequestPasswordDialog;

public class PasswdHelper {

    private static final String TAG = PasswdHelper.class.getSimpleName();

    private static final String REQUEST_DB_DIALOG_FRAGMENT_TAG = "RequestPasswordDialog";

    private static String passwd = "";

    public static boolean getWritableDatabase(Activity activity, RequestDbListener listener,
                                              int requestId) {
        return getWritableDatabase(activity, listener, requestId, true);
    }

    private static boolean getWritableDatabase(final Activity activity, RequestDbListener listener,
                                               int requestId, boolean newRequest) {
        DbHelper dbHelper = new DbHelper(activity);
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase(passwd);
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (DbHelper.UnsupportedUpgradeException e) {
            // Unsupported db version: notify user and close app
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.error_upgrade_title)
                    .setMessage(e.newVersion < e.oldVersion
                            ? R.string.error_downgrade_summary
                            : R.string.error_upgrade_summary)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    })
                    .show();
            // Not a password error
            return true;
        }
        if (db != null) {
            listener.receiveWritableDatabase(db, requestId);
            return true;
        } else if (newRequest) {
            FragmentManager fragmentManager = activity.getFragmentManager();
            RequestPasswordDialog requestPasswordDialog = (RequestPasswordDialog) fragmentManager
                    .findFragmentByTag(REQUEST_DB_DIALOG_FRAGMENT_TAG);
            if (requestPasswordDialog == null) {
                requestPasswordDialog = new RequestPasswordDialog();
                requestPasswordDialog.show(fragmentManager, REQUEST_DB_DIALOG_FRAGMENT_TAG);
            }
            requestPasswordDialog.addRequest(listener, requestId);
        }
        return false;
    }

    public static boolean getWritableDatabase(Activity activity, RequestDbListener listener,
                                              int requestId, String newPasswd, boolean newRequest) {
        passwd = newPasswd;
        return getWritableDatabase(activity, listener, requestId, newRequest);
    }

    public static boolean isPasswordUnset() {
        return "".equals(passwd);
    }

    public static boolean doesPasswordMatch(String passwd) {
        return PasswdHelper.passwd.equals(passwd);
    }

    public static void setPasswd(Context context, String oldPasswd, String newPasswd) {
        if (oldPasswd.equals(newPasswd)) {
            Log.w(TAG, "setPasswd: ignoring call with identical old and new password");
            return;
        }
        if (!passwd.equals(oldPasswd)) {
            Log.e(TAG, "setPasswd: wrong old password");
            return;
        }
        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase(passwd);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        if (db == null) {
            Log.e(TAG, "setPasswd: old password denied by database");
            return;
        }

        if ("".equals(oldPasswd) || "".equals(newPasswd)) {
            // Add/remove decryption: simple changePassword() call is not enough,
            // migrate to a new db instead
            File dbFile = context.getDatabasePath(DbHelper.NAME);
            File dbCopyFile = context.getDatabasePath(DbHelper.NAME_COPY);
            dbCopyFile.delete();
            SQLiteDatabase dbCopy = SQLiteDatabase.openOrCreateDatabase(dbCopyFile, newPasswd, null);
            dbCopy.setVersion(DbHelper.VERSION);
            db.rawExecSQL("PRAGMA key = \'" + oldPasswd + "\';");
            db.rawExecSQL("ATTACH DATABASE \'" + dbCopyFile +
                    "\' AS encrypted KEY \'" + newPasswd + "\';");
            db.rawExecSQL("SELECT sqlcipher_export(\'encrypted\');");
            db.rawExecSQL("DETACH DATABASE encrypted;");
            dbFile.delete();
            dbCopyFile.renameTo(dbFile);
        } else {
            db.changePassword(newPasswd);
        }
        db.close();
        dbHelper.close();
        passwd = newPasswd;
    }

    public static void cloneDb(SQLiteDatabase db, File outFile) {
        String exportPasswd = passwd;
        outFile.delete();
        SQLiteDatabase dbCopy = SQLiteDatabase.openOrCreateDatabase(outFile, exportPasswd, null);
        dbCopy.setVersion(DbHelper.VERSION);
        db.rawExecSQL("PRAGMA key = \'" + passwd + "\';");
        db.rawExecSQL("ATTACH DATABASE \'" + outFile +
                "\' AS encrypted KEY \'" + exportPasswd + "\';");
        db.rawExecSQL("SELECT sqlcipher_export(\'encrypted\');");
        db.rawExecSQL("DETACH DATABASE encrypted;");
        db.close();
    }

    public interface RequestDbListener {
        void receiveWritableDatabase(SQLiteDatabase db, int requestId);
    }

    static void onExit() {
        // Reset password
        passwd = "";
    }
}
