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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.data.DbContract;
import de.spiritcroc.ownlog.data.LoadLogItemAttachmentsTask;
import de.spiritcroc.ownlog.data.LogItem;

public class LogAttachmentsEditFragment extends LogAttachmentsShowFragment
        implements View.OnClickListener {

    private static final String TAG = LogAttachmentsEditFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    // Saved instance state bundle keys
    private static final String KEY_REQUEST_ATTACHMENT_POSITION =
            LogAttachmentsEditFragment.class.getName() + ".request_attachment_position";
    private static final String KEY_TMP_ATTACHMENT_NAME =
            LogAttachmentsEditFragment.class.getName() + ".received_attachment_intent";
    private static final String KEY_ADD_URI =
            LogAttachmentsEditFragment.class.getName() + ".add_uri";

    private static final int RESULT_CODE_ADD_ATTACHMENT = 1;
    private static final int DB_REQUEST_ADD_ATTACHMENT = 100; // Don't interfere with superclass
    private static final int DB_REQUEST_EDIT_ATTACHMENT = 101;
    private static final int DB_REQUEST_DELETE_ATTACHMENT = 102;

    private static final int ATTACHMENT_POSITION_NONE = -1;
    private static final int ATTACHMENT_POSITION_NEW = -2;

    private static final String RENAME_FRAGMENT_TAG = "LogAttachmentRenameFragment";

    private View mAddAttachmentButton;

    private int mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
    private String mTmpAttachmentName;
    private Uri mAddUri;
    private LogAttachmentRenameFragment mRenameFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mAddAttachmentButton = view.findViewById(R.id.add_attachment_button);
        mAddAttachmentButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            mRequestedAttachmentPosition =
                    savedInstanceState.getInt(KEY_REQUEST_ATTACHMENT_POSITION);
            mTmpAttachmentName = savedInstanceState.getString(KEY_TMP_ATTACHMENT_NAME);
            mRenameFragment = (LogAttachmentRenameFragment) getFragmentManager()
                    .findFragmentByTag(RENAME_FRAGMENT_TAG);
            mAddUri = savedInstanceState.getParcelable(KEY_ADD_URI);
            if (mRenameFragment != null) {
                if (mRequestedAttachmentPosition != ATTACHMENT_POSITION_NONE) {
                    mRenameFragment.setRenameListener(
                            mRequestedAttachmentPosition == ATTACHMENT_POSITION_NEW
                                    ? getInitialNameListener()
                                    : getRenameListener()
                    );
                }
            }
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_REQUEST_ATTACHMENT_POSITION, mRequestedAttachmentPosition);
        outState.putString(KEY_TMP_ATTACHMENT_NAME, mTmpAttachmentName);
        outState.putParcelable(KEY_ADD_URI, mAddUri);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.log_attachments_edit;
    }

    @Override
    public void onClick(View view) {
        if (view == mAddAttachmentButton) {
            openAddAttachmentChooser();
        }
    }

    protected class AttachmentViewHolder extends LogAttachmentsShowFragment.AttachmentViewHolder {
        public View mRenameButton;
        public View mDeleteButton;
        private abstract class ButtonClickListener implements View.OnClickListener {
            public int mPosition;
            public ButtonClickListener(int position) {
                mPosition = position;
            }
        }
        private ButtonClickListener mButtonClickListener;

        public AttachmentViewHolder(View view) {
            super(view);
            mRenameButton = view.findViewById(R.id.rename_attachment_button);
            mDeleteButton = view.findViewById(R.id.delete_attachment_button);
            mButtonClickListener = new ButtonClickListener(-1) {
                @Override
                public void onClick(View view) {
                    if (mRequestedAttachmentPosition != ATTACHMENT_POSITION_NONE) {
                        Log.w(TAG, "Discarding click for attachment " + mPosition
                                + ": already working with attachment "
                                + mRequestedAttachmentPosition);
                        return;
                    }
                    mRequestedAttachmentPosition = mPosition;
                    if (view == mRenameButton) {
                        Log.d(TAG, "Prompting rename of " + mPosition);
                        promptRenameAttachment();
                    } else if (view == mDeleteButton) {
                        Log.d(TAG, "Prompting delete of " + mPosition);
                        promptDeleteAttachment();
                    } else {
                        Log.e(TAG, "Attachment " + mPosition + " clicked on unknown view");
                        mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
                    }
                }
            };
            mRenameButton.setOnClickListener(mButtonClickListener);
            mDeleteButton.setOnClickListener(mButtonClickListener);
        }
    }

    private void promptRenameAttachment() {
        mRenameFragment = new LogAttachmentRenameFragment().setRenameListener(getRenameListener());
        mRenameFragment.show(getFragmentManager(), RENAME_FRAGMENT_TAG);
    }

    private LogAttachmentRenameFragment.RenameListener getRenameListener() {
        return new LogAttachmentRenameFragment.RenameListener() {
            @Override
            public String getCurrentName() {
                return getAttachment(mRequestedAttachmentPosition).name;
            }

            @Override
            public void trySetName(String name) {
                mTmpAttachmentName = name;
                PasswdHelper.getWritableDatabase(getActivity(), LogAttachmentsEditFragment.this,
                        DB_REQUEST_EDIT_ATTACHMENT);
            }

            @Override
            public void onCancelRename() {
                mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
            }
        };
    }

    private void promptInitialNameAttachment() {
        if (mRequestedAttachmentPosition != ATTACHMENT_POSITION_NONE) {
            Log.w(TAG, "Discarding adding attachment: already working with attachment "
                    + mRequestedAttachmentPosition);
            return;
        }
        mRequestedAttachmentPosition = ATTACHMENT_POSITION_NEW;
        mRenameFragment = new LogAttachmentRenameFragment()
                .setRenameListener(getInitialNameListener());
        mRenameFragment.show(getFragmentManager(), RENAME_FRAGMENT_TAG);
    }

    private LogAttachmentRenameFragment.RenameListener getInitialNameListener() {
        return new LogAttachmentRenameFragment.RenameListener() {
            @Override
            public String getCurrentName() {
                return mTmpAttachmentName;
            }

            @Override
            public void trySetName(String name) {
                mTmpAttachmentName = name;
                PasswdHelper.getWritableDatabase(getActivity(),
                        LogAttachmentsEditFragment.this, DB_REQUEST_ADD_ATTACHMENT);
            }

            @Override
            public void onCancelRename() {
                mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
            }
        };
    }

    private void promptDeleteAttachment() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_attachment_title)
                .setMessage(getString(R.string.dialog_delete_attachment,
                        getAttachment(mRequestedAttachmentPosition).name))
                .setPositiveButton(R.string.dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                PasswdHelper.getWritableDatabase(getActivity(),
                                        LogAttachmentsEditFragment.this,
                                        DB_REQUEST_DELETE_ATTACHMENT);
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
                    }
                })
                .show();
    }

    private void editAttachment(SQLiteDatabase db) {
        String errorMsg = getNameError(db);
        if (errorMsg != null) {
            db.close();
            mRenameFragment.errorNameInvalid(errorMsg);
            return;
        }
        if (mRequestedAttachmentPosition == ATTACHMENT_POSITION_NONE) {
            Log.e(TAG, "editAttachment: nothing selected");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME, mTmpAttachmentName);
        String selection = DbContract.LogAttachment._ID + " = ?";
        String[] selectionArgs = {String.valueOf(getAttachment(mRequestedAttachmentPosition).id)};
        db.update(DbContract.LogAttachment.TABLE, values, selection, selectionArgs);
        db.close();
        mRenameFragment.verifyNameSet();
        mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
        loadContent();
    }

    private void deleteAttachment(SQLiteDatabase db) {
        if (mRequestedAttachmentPosition == ATTACHMENT_POSITION_NONE) {
            Log.e(TAG, "deleteAttachment: nothing selected");
            return;
        }
        String selection = DbContract.LogAttachment._ID + " = ?";
        db.delete(DbContract.LogAttachment.TABLE, selection,
                new String[]{String.valueOf(getAttachment(mRequestedAttachmentPosition).id)});
        db.close();
        mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
        loadContent();
    }

    private String getNameError(SQLiteDatabase db) {
        if (TextUtils.isEmpty(mTmpAttachmentName) ||
                mTmpAttachmentName.lastIndexOf('.') == 0) {
            return getString(R.string.error_should_not_be_empty);
        }
        String shouldNotExist = DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME
                + " = '" + mTmpAttachmentName + "'";
        if (mRequestedAttachmentPosition >= 0) {
            shouldNotExist += " AND " + DbContract.LogAttachment._ID
                    + " != " + getAttachment(mRequestedAttachmentPosition).id;
        }
        if (LoadLogItemAttachmentsTask.loadAttachments(db, shouldNotExist,
                new String[]{DbContract.LogAttachment._ID},
                LoadLogItemAttachmentsTask.getSortOrder()).isEmpty()) {
            return null;
        } else {
            return getString(R.string.edit_log_attachment_name_exists);
        }
    }

    @Override
    protected boolean isAttachmentClickEnabled() {
        return false;
    }

    @Override
    protected AttachmentViewHolder onCreateAttachmentViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.log_attachment_edit, parent, false);
        return new AttachmentViewHolder(v);
    }

    @Override
    protected void onBindAttachmentViewHolder(
            LogAttachmentsShowFragment.AttachmentViewHolder holder, int position) {
        super.onBindAttachmentViewHolder(holder, position);
        AttachmentViewHolder holder1 = (AttachmentViewHolder) holder;
        holder1.mButtonClickListener.mPosition = position;
    }

    private void openAddAttachmentChooser() {
        // TODO what if none installed?
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, RESULT_CODE_ADD_ATTACHMENT);
    }

    private String getAttachmentNameProposal(Intent intent) {
        Uri uri = intent.getData();
        Cursor infoCursor = getActivity().getContentResolver().query(uri, null, null, null,
                null);
        String name = null;
        if (infoCursor != null) {
            infoCursor.moveToFirst();
            try {
                name = infoCursor.getString(
                        infoCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } catch (Exception e) {
                e.printStackTrace();
            }
            infoCursor.close();
        }
        if (name == null) {
            Log.w(TAG, "addAttachment: query filename unsuccessful");
            return getString(R.string.edit_log_attachment_name_default);
        }
        return name;
    }

    private void addAttachment(SQLiteDatabase db) {
        String errorMsg = getNameError(db);
        if (errorMsg != null) {
            db.close();
            mRenameFragment.errorNameInvalid(errorMsg);
            return;
        }

        InputStream inputStream = null;
        BufferedInputStream in = null;

        String type = getActivity().getContentResolver().getType(mAddUri);
        if (DEBUG) Log.d(TAG, "Inserting file " + mTmpAttachmentName + " with type " + type);
        try {
            inputStream = getActivity().getContentResolver().openInputStream(mAddUri);
            if (inputStream == null) {
                throw new IOException("Could not open inputStream from intent");
            }
            in = new BufferedInputStream(inputStream);
            byte[] data = new byte[in.available()];
            in.read(data);
            ContentValues values = new ContentValues();
            values.put(DbContract.LogAttachment._ID, LogItem.Attachment.generateId());
            values.put(DbContract.LogAttachment.COLUMN_LOG, getLogId());
            values.put(DbContract.LogAttachment.COLUMN_ATTACHMENT_NAME, mTmpAttachmentName);
            values.put(DbContract.LogAttachment.COLUMN_ATTACHMENT_TYPE, type);
            values.put(DbContract.LogAttachment.COLUMN_ATTACHMENT_DATA, data);
            db.insert(DbContract.LogAttachment.TABLE, "null", values);
            if (DEBUG) Log.d(TAG, "Inserted new attachment");
        } catch (IOException e) {
            Toast.makeText(getActivity(), R.string.error_io_in, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            Toast.makeText(getActivity(), R.string.error_attachment_too_big, Toast.LENGTH_LONG)
                    .show();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            db.close();
        }
        mRenameFragment.verifyNameSet();
        mRequestedAttachmentPosition = ATTACHMENT_POSITION_NONE;
        loadContent();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case RESULT_CODE_ADD_ATTACHMENT:
                    if (data != null) {
                        mAddUri = data.getData();
                        mTmpAttachmentName = getAttachmentNameProposal(data);
                        promptInitialNameAttachment();
                    }
                    break;
            }
        }
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        switch (requestId) {
            case DB_REQUEST_ADD_ATTACHMENT:
                addAttachment(db);
                break;
            case DB_REQUEST_EDIT_ATTACHMENT:
                editAttachment(db);
                break;
            case DB_REQUEST_DELETE_ATTACHMENT:
                deleteAttachment(db);
                break;
            default:
                super.receiveWritableDatabase(db, requestId);
        }
    }
}
