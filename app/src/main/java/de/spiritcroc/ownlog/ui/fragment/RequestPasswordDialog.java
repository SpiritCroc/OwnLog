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

package de.spiritcroc.ownlog.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;

public class RequestPasswordDialog extends DialogFragment
        implements PasswdHelper.RequestDbListener {

    private static final String TAG = RequestPasswordDialog.class.getSimpleName();

    private static final int DB_BATCH_REQUEST = 0;

    private ArrayList<Request> requests = new ArrayList<>();

    private class Request {
        PasswdHelper.RequestDbListener listener;
        int requestId;
        Request(PasswdHelper.RequestDbListener listener, int requestId) {
            this.listener = listener;
            this.requestId = requestId;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view =
                activity.getLayoutInflater().inflate(R.layout.dialog_request_password, null);

        final EditText editPassword = (EditText) view.findViewById(R.id.edit_password);

        setCancelable(false);

        final AlertDialog alertDialog = builder.setTitle(R.string.dialog_request_password)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (PasswdHelper.getWritableDatabase(getActivity(),
                                        RequestPasswordDialog.this, DB_BATCH_REQUEST,
                                        editPassword.getText().toString(), false)) {
                                    dismiss();
                                } else {
                                    editPassword.setError(getString(R.string.edit_wrong_password));
                                }
                            }
                        });
            }
        });

        // Edit text requires user interaction, so show keyboard
        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return alertDialog;
    }

    public void addRequest(PasswdHelper.RequestDbListener listener, int requestId) {
        requests.add(new Request(listener, requestId));
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        if (requestId == DB_BATCH_REQUEST) {
            for (Request r: requests) {
                r.listener.receiveWritableDatabase(db, r.requestId);
            }
        } else {
            Log.e(TAG, "receiveWritableDatabase: unknown requestId " + requestId);
        }
    }
}
