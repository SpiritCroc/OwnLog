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

package de.spiritcroc.ownlog.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;

import de.spiritcroc.ownlog.FileHelper;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;

public class ExportDialog extends DialogFragment implements PasswdHelper.RequestDbListener {

    private static final String TAG = ExportDialog.class.getSimpleName();

    private static final int DB_REQUEST_EXPORT = 1;

    private TextView mMessage;
    private ProgressBar mProgressBar;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View progressView = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_progess_indeterminate, null);
        mMessage = progressView.findViewById(R.id.progress_message);
        mMessage.setText(R.string.export_dialog_message);
        mProgressBar = progressView.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);
        setCancelable(false);
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.export_dialog_title)
                .setView(progressView)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PasswdHelper.getWritableDatabase(getActivity(), ExportDialog.this,
                                        DB_REQUEST_EXPORT);
                            }
                        });
            }
        });
        return alertDialog;
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        if (requestId == DB_REQUEST_EXPORT) {
            new ExportTask(db).execute();
        } else {
            Log.e(TAG, "receiveWritableDatabase: unknown requestId " + requestId);
        }
    }

    private class ExportTask extends AsyncTask<Void, Void, File> {
        private SQLiteDatabase mDb;
        private ExportTask(SQLiteDatabase db) {
            mDb = db;
        }
        @Override
        protected void onPreExecute() {
            AlertDialog dialog = (AlertDialog) getDialog();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
            mMessage.setText(R.string.export_dialog_progress_message);
            mProgressBar.setVisibility(View.VISIBLE);
        }
        @Override
        protected File doInBackground(Void... nothing) {
            return FileHelper.generateExport(getActivity(), mDb);
        }
        @Override
        protected void onPostExecute(File result) {
            if (result == null) {
                Toast.makeText(getActivity(), R.string.error_internal, Toast.LENGTH_LONG).show();
            } else {
                ShareCompat.IntentBuilder.from(getActivity())
                        .addStream(FileHelper.getFileShare(getActivity(), result))
                        .setType("app/zip")
                        .startChooser();
            }
            dismiss();
        }
    }
}
