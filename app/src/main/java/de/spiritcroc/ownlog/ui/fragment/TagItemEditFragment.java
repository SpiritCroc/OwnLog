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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.data.DbContract;
import de.spiritcroc.ownlog.data.LoadTagItemsTask;
import de.spiritcroc.ownlog.data.TagItem;

public class TagItemEditFragment extends DialogFragment implements PasswdHelper.RequestDbListener {

    private static final String TAG = LogItemEditFragment.class.getSimpleName();

    // Saved instance state bundle keys
    private static final String KEY_ADD_ITEM =
            TagItemEditFragment.class.getName() + ".add_item";
    private static final String KEY_EDIT_ITEM_ID =
            TagItemEditFragment.class.getName() + ".edit_item_id";
    private static final String KEY_INIT_NAME =
            LogFilterEditFragment.class.getName() + ".init_name";
    private static final String KEY_SET_NAME =
            LogFilterEditFragment.class.getName() + ".set_name";
    private static final String KEY_INIT_DESCRIPTION =
            LogFilterEditFragment.class.getName() + ".init_description";
    private static final String KEY_SET_DESCRIPTION =
            LogFilterEditFragment.class.getName() + ".set_description";
    private static final String KEY_SHOULD_HIDE_KEYBOARD_ON_DISMISS =
            LogFilterEditFragment.class.getName() + ".hide_keyboard";

    private static final int DB_REQUEST_LOAD = 1;
    private static final int DB_REQUEST_SAVE = 2;
    private static final int DB_REQUEST_DELETE = 3;

    private boolean mAddItem = true;
    private long mEditItemId = -1;
    private boolean mShouldHideKeyboardOnExit = false;

    // Remember the initial values to check whether anything needs saving
    private String mInitName = "";
    private String mInitDescription = "";

    private EditText mEditTagName;
    private EditText mEditTagDescription;

    public TagItemEditFragment setEditItemId(long id) {
        mAddItem = false;
        mEditItemId = id;
        return this;
    }

    /**
     * Hide keyboard again on dismiss: if a dialog is shown behind this dialog, keyboard will by
     * shown behind that dialog otherwise until it all dialogs are closed.
     * onDismiss() and onCancel() seem to be too late to do this, so do it manually.
     * Unfortunately, I couldn't figure out how to do it for canceled dialogs; but that's not
     * enough of an issue to forbid cancelling. If a user has a problem with it, he can click
     * on cancel instead of outside of the dialog
     *
     * @param hide
     * Whether to hide the keyboard on dismiss. Set true if caller is a dialog without priority on
     * text input
     */
    public TagItemEditFragment setShouldHideKeyboardOnDismiss(boolean hide) {
        mShouldHideKeyboardOnExit = hide;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view =
                activity.getLayoutInflater().inflate(R.layout.tag_edit_item, null);

        mEditTagName = (EditText) view.findViewById(R.id.name_edit);
        mEditTagDescription = (EditText) view.findViewById(R.id.description_edit);

        boolean restoredValues = restoreValues(savedInstanceState);

        builder.setTitle(mAddItem ? R.string.title_tag_item_add : R.string.title_tag_item_edit)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only dismiss (and hide keyboard)
                        hideKeyboard();
                    }
                });
        if (!mAddItem) {
            builder.setNeutralButton(R.string.dialog_delete, null);
        }
        final AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                saveChanges();
                            }
                        });
                if (!mAddItem) {
                    alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    promptDelete();
                                }
                            });
                }
            }
        });

        if (!restoredValues) {
            // Edit text requires user interaction, so show keyboard
            alertDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            if (mAddItem) {
                initValues();
            } else {
                loadContent();
            }
        }

        return alertDialog;
    }

    @Override
    public void dismiss() {
        hideKeyboard();
        super.dismiss();
    }

    private void hideKeyboard() {
        if (!mShouldHideKeyboardOnExit) {
            return;
        }
        ((InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(mEditTagName.getWindowToken(), 0);
    }

    private void initValues() {
        mEditTagName.setText(mInitName);
        mEditTagDescription.setText(mInitDescription);
    }

    private boolean restoreValues(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return false;
        }
        try {
            if (savedInstanceState.containsKey(KEY_ADD_ITEM)) {
                mAddItem = savedInstanceState.getBoolean(KEY_ADD_ITEM);
                mEditItemId = savedInstanceState.getLong(KEY_EDIT_ITEM_ID);
                mInitName = savedInstanceState.getString(KEY_INIT_NAME);
                mEditTagName.setText(savedInstanceState.getString(KEY_SET_NAME));
                mInitDescription = savedInstanceState.getString(KEY_INIT_DESCRIPTION);
                mEditTagDescription.setText(savedInstanceState.getString(KEY_SET_DESCRIPTION));
                mShouldHideKeyboardOnExit =
                        savedInstanceState.getBoolean(KEY_SHOULD_HIDE_KEYBOARD_ON_DISMISS);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ADD_ITEM, mAddItem);
        outState.putLong(KEY_EDIT_ITEM_ID, mEditItemId);
        outState.putString(KEY_INIT_NAME, mInitName);
        outState.putString(KEY_SET_NAME, mEditTagName.getText().toString());
        outState.putString(KEY_INIT_DESCRIPTION, mInitDescription);
        outState.putString(KEY_SET_DESCRIPTION, mEditTagDescription.getText().toString());
        outState.putBoolean(KEY_SHOULD_HIDE_KEYBOARD_ON_DISMISS, mShouldHideKeyboardOnExit);
    }

    private void loadContent() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_LOAD);
    }

    private void loadContent(SQLiteDatabase db) {
        new LoadContentTask(db).execute();
    }

    private void saveChanges() {
        if (noChangesPresent()) {
            dismiss();
        } else {
            PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_SAVE);
        }
    }

    private void saveChanges(SQLiteDatabase db) {
        if (!isNameValid(db)) {
            db.close();
            return;
        }
        TagItem result = new TagItem(mEditItemId, mEditTagName.getText().toString(),
                mEditTagDescription.getText().toString());
        ContentValues values = new ContentValues();
        values.put(DbContract.Tag.COLUMN_NAME, result.name);
        values.put(DbContract.Tag.COLUMN_DESCRIPTION, result.description);
        if (mAddItem) {
            values.put(DbContract.Tag._ID, TagItem.generateId());
            result.id = db.insert(DbContract.Tag.TABLE, "null", values);
        } else {
            result.id = mEditItemId;
            String selection = DbContract.Log._ID + " = ?";
            String[] selectionArgs = {String.valueOf(mEditItemId)};
            db.update(DbContract.Tag.TABLE, values, selection, selectionArgs);
        }
        db.close();
        dismiss();
        if (mAddItem) {
            // Notify about added tag
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                    new Intent(Constants.EVENT_TAG_UPDATE)
                            .putExtra(Constants.EXTRA_TAG_ITEM, result)
                            .putExtra(Constants.EXTRA_TAG_ACTION, Constants.TAG_ACTION_ADD)
            );
        } else {
            // Notify about edited tag
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                    new Intent(Constants.EVENT_TAG_UPDATE)
                            .putExtra(Constants.EXTRA_TAG_ITEM, result)
                            .putExtra(Constants.EXTRA_TAG_ACTION, Constants.TAG_ACTION_EDIT)
            );
        }
    }

    private void promptDelete() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dialog_delete_tag_entry_title, mInitName))
                .setMessage(R.string.dialog_delete_tag_entry_message)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteItem();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Only dismiss dialog
                    }
                })
                .show();
    }

    private void deleteItem() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_DELETE);
    }

    private void deleteItem(SQLiteDatabase db) {
        String selection = DbContract.Tag._ID + " = ?";
        String[] selectionArgs = {String.valueOf(mEditItemId)};
        db.delete(DbContract.Tag.TABLE, selection, selectionArgs);
        db.close();
        dismiss();
        // Notify about deleted tag
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent(Constants.EVENT_TAG_UPDATE)
                        .putExtra(Constants.EXTRA_TAG_ITEM, new TagItem(mEditItemId))
                        .putExtra(Constants.EXTRA_TAG_ACTION, Constants.TAG_ACTION_DELETE)
        );
        Toast.makeText(getActivity(),
                // Should be impossible to create/edit a tag with empty name, so we can assume
                // here mInitName is not empty
                getString(R.string.dialog_deleted_tag_entry_toast_title, mInitName),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        switch (requestId) {
            case DB_REQUEST_LOAD:
                loadContent(db);
                break;
            case DB_REQUEST_SAVE:
                saveChanges(db);
                break;
            case DB_REQUEST_DELETE:
                deleteItem(db);
                break;
        }
    }

    private boolean noChangesPresent() {
        return mInitName.equals(mEditTagName.getText().toString())
                && mInitDescription.equals(mEditTagDescription.getText().toString());
    }

    private boolean isNameValid(SQLiteDatabase db) {
        String name = mEditTagName.getText().toString();
        if ("".equals(name)) {
            mEditTagName.setError(getString(R.string.error_should_not_be_empty));
            return false;
        }
        String shouldNotExist = DbContract.Tag.COLUMN_NAME + " = '" + name + "' AND "
                + DbContract.Tag._ID + " != " + mEditItemId;
        if (LoadTagItemsTask.loadAvailableTags(db, shouldNotExist).isEmpty()) {
            return true;
        } else {
            mEditTagName.setError(getString(R.string.error_tag_with_name_exists));
            return false;
        }
    }

    private class LoadContentTask extends LoadTagItemsTask {

        LoadContentTask(SQLiteDatabase db) {
            super(db);
        }

        @Override
        protected void onPostExecute(ArrayList<TagItem> result) {
            if (result.isEmpty()) {
                if (!mAddItem) {
                    Log.e(TAG, "DB response is empty for id " + mEditItemId);
                    dismiss();
                } // else: we're done
            } else if (result.size() > 1) {
                Log.e(TAG, "Too many objects found for id " + mEditItemId);
            } else {
                TagItem tagItem = result.get(0);
                mInitName = tagItem.name;
                mInitDescription = tagItem.description;
                initValues();
            }
        }

        @Override
        protected String getSelection() {
            return DbContract.Log._ID + " = " + mEditItemId;
        }
    }
}
