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
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;

public class SetPasswordDialog extends DismissDialogFragment {

    private boolean mPasswordUnset;
    private String oldPasswd;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view =
                activity.getLayoutInflater().inflate(R.layout.dialog_set_password, null);

        mPasswordUnset = PasswdHelper.isPasswordUnset();

        final EditText oldPassword = (EditText) view.findViewById(R.id.old_password);
        final EditText newPassword = (EditText) view.findViewById(R.id.new_password);
        final EditText confirmPassword = (EditText) view.findViewById(R.id.confirm_password);

        if (mPasswordUnset) {
            view.findViewById(R.id.old_password_layout).setVisibility(View.GONE);
        }

        final AlertDialog alertDialog = builder
                .setTitle(mPasswordUnset
                        ? R.string.dialog_set_password
                        : R.string.dialog_change_password)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only dismiss
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
                                oldPasswd = oldPassword.getText().toString();
                                String newPasswd = newPassword.getText().toString();
                                String confirmPasswd = confirmPassword.getText().toString();
                                if (!mPasswordUnset && !PasswdHelper.doesPasswordMatch(oldPasswd)) {
                                    oldPassword.setError(getString(R.string.edit_wrong_password));
                                    return;
                                }
                                if (!newPasswd.equals(confirmPasswd)) {
                                    confirmPassword.setError(
                                            getString(R.string.edit_wrong_confirm_password));
                                    return;
                                }
                                if (!mPasswordUnset && "".equals(newPasswd)) {
                                    unsetPassword();
                                    return;
                                }
                                PasswdHelper.setPasswd(getActivity(), oldPasswd, newPasswd);
                                dismiss();
                            }
                        });
            }
        });

        // Edit text requires user interaction, so show keyboard
        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return alertDialog;
    }

    private void unsetPassword() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.dialog_remove_password)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PasswdHelper.setPasswd(getActivity(), oldPasswd, "");
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Only dismiss unset-password-dialog
                    }
                })
                .show();
    }
}
