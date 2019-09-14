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

package de.spiritcroc.ownlog.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import de.spiritcroc.ownlog.R;

public class LogAttachmentRenameFragment extends DialogFragment {
    private static final String TAG = LogAttachmentRenameFragment.class.getSimpleName();

    // Saved instance state bundle keys
    private static final String KEY_SET_NAME =
            LogAttachmentRenameFragment.class.getName() + ".set_name";

    private EditText mEditName;
    private TextView mShowNameExtension;

    private RenameListener mListener;

    public LogAttachmentRenameFragment setRenameListener(RenameListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater()
                .inflate(R.layout.log_attachment_rename, null);

        mEditName = (EditText) view.findViewById(R.id.edit_name);
        mShowNameExtension = (TextView) view.findViewById(R.id.show_file_extension);

        if (savedInstanceState == null) {
            setName(mListener.getCurrentName());
        } else {
            try {
                setName(savedInstanceState.getString(KEY_SET_NAME));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        builder.setTitle(R.string.edit_log_attachment_rename_title)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onCancelRename();
                    }
                });
        final AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                saveChanges();
                            }
                        });
            }
        });

        // EditText requires user interaction, so show keyboard
        showKeyboard(alertDialog);

        return alertDialog;
    }

    private void setName(String name) {
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex >= 0) {
            mShowNameExtension.setVisibility(View.VISIBLE);
            mShowNameExtension.setText(name.substring(extensionIndex));
            mEditName.setText(name.substring(0, extensionIndex));
        } else {
            mShowNameExtension.setVisibility(View.GONE);
            mShowNameExtension.setText("");
            mEditName.setText(name);
        }
    }

    private String getName() {
        return mEditName.getText().toString() + mShowNameExtension.getText().toString();
    }

    public void verifyNameSet() {
        dismiss();
    }

    public void errorNameInvalid(String msg) {
        mEditName.setEnabled(true);
        mEditName.setError(msg);
    }

    private void saveChanges() {
        mEditName.setEnabled(false);
        mListener.trySetName(getName());
    }

    public interface RenameListener {
        String getCurrentName();

        /**
         * Call verifyNameSet() on success or errorNameInvalid() on failure after
         * call of trySetName
         */
        void trySetName(String name);

        void onCancelRename();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mListener.onCancelRename();
    }

    private void showKeyboard(Dialog alertDialog) {
        alertDialog.getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
