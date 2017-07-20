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

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.DateFormatter;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.Settings;
import de.spiritcroc.ownlog.data.DbContract;
import de.spiritcroc.ownlog.data.LoadLogItemsTask;
import de.spiritcroc.ownlog.data.LoadTagItemsTask;
import de.spiritcroc.ownlog.data.LogItem;
import de.spiritcroc.ownlog.data.TagItem;
import de.spiritcroc.ownlog.ui.activity.SingleFragmentActivity;
import de.spiritcroc.ownlog.ui.view.EditTagsView;

public class LogItemEditFragment extends BaseFragment implements View.OnClickListener,
        View.OnLongClickListener, DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener, PasswdHelper.RequestDbListener,
        EditTagsView.EditTagsProvider {

    private static final String TAG = LogItemEditFragment.class.getSimpleName();

    // Saved instance state bundle keys
    private static final String KEY_ADD_ITEM =
            LogItemEditFragment.class.getName() + ".add_item";
    private static final String KEY_TEMPORARY_EXISTENCE =
            LogItemEditFragment.class.getName() + ".temporary_existence";
    private static final String KEY_INIT_TITLE =
            LogItemEditFragment.class.getName() + ".init_title";
    private static final String KEY_SET_TITLE =
            LogItemEditFragment.class.getName() + ".set_title";
    private static final String KEY_INIT_CONTENT =
            LogItemEditFragment.class.getName() + ".init_content";
    private static final String KEY_SET_CONTENT =
            LogItemEditFragment.class.getName() + ".set_content";
    private static final String KEY_INIT_TIME =
            LogItemEditFragment.class.getName() + ".init_time";
    private static final String KEY_SET_TIME =
            LogItemEditFragment.class.getName() + ".set_time";
    private static final String KEY_INIT_TAGS =
            LogItemEditFragment.class.getName() + ".init_tags";
    private static final String KEY_SET_TAGS =
            LogItemEditFragment.class.getName() + ".set_tags";
    private static final String KEY_AVAILABLE_TAGS =
            LogItemEditFragment.class.getName() + ".available_tags";

    private static final String FRAGMENT_TAG_ATTACHMENTS = TAG + ".attachments";

    // DB request codes
    private static final int DB_REQUEST_LOAD = 1;
    private static final int DB_REQUEST_SAVE = 2;
    private static final int DB_REQUEST_SAVE_FOR_ATTACHMENTS = 3;
    private static final int DB_REQUEST_DELETE = 4;

    private boolean mAddItem = true;
    private boolean mTemporaryExistence = false;
    private long mEditItemId = -1;

    // Remember the initial values to check whether anything needs saving
    private long mInitTime = System.currentTimeMillis();
    private String mInitTitle = "";
    private String mInitContent = "";
    private ArrayList<TagItem> mInitTags = new ArrayList<>();
    private ArrayList<TagItem> mAvailableTags = new ArrayList<>();
    private ArrayList<TagItem> mSetTags = null;

    private Calendar mTime;

    private View mEditTime;
    private EditText mEditLogTitle;
    private EditText mEditLogContent;
    private EditTagsView mEditTagsView;
    private BottomSheetBehavior mBottomSheetBehavior;
    private LogAttachmentsEditFragment mAttachmentsFragment;

    /**
     * @param logItem
     * The LogItem to edit. If null, a new item will be created.
     */
    public static void show(Context context, @Nullable LogItem logItem) {
        Intent intent = new Intent(context, SingleFragmentActivity.class)
                .putExtra(Constants.EXTRA_FRAGMENT_CLASS,
                        LogItemEditFragment.class.getName());
        if (logItem != null) {
            Bundle fragmentArgs = new Bundle();
            fragmentArgs.putLong(Constants.EXTRA_LOG_ITEM_ID, logItem.id);
            intent.putExtra(Constants.EXTRA_FRAGMENT_BUNDLE, fragmentArgs);
        }
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mEditItemId = savedInstanceState.getLong(Constants.EXTRA_LOG_ITEM_ID, -1);
            mAddItem = savedInstanceState.getBoolean(KEY_ADD_ITEM, mEditItemId == -1);
            mTemporaryExistence = savedInstanceState.getBoolean(KEY_TEMPORARY_EXISTENCE, false);
        } else if (getArguments() == null) {
            mAddItem = true;
        } else {
            mEditItemId = getArguments().getLong(Constants.EXTRA_LOG_ITEM_ID, -1);
            mAddItem = mEditItemId == -1;
        }

        // We will be using the the keyboard more in this screen. Don't do any awkward content
        // moving around; just resize the content to fit into the visible window.
        getActivity().getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.log_edit_item, container, false);
        mEditTime = view.findViewById(R.id.date_button);
        mEditLogTitle = (EditText) view.findViewById(R.id.title_edit);
        mEditLogContent = (EditText) view.findViewById(R.id.content_edit);
        mEditTagsView = (EditTagsView) view.findViewById(R.id.edit_tags_view);
        View attachmentsStubView = view.findViewById(R.id.attachments_stub);
        mBottomSheetBehavior = BottomSheetBehavior.from(attachmentsStubView);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN
                        || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // Allow editing, no bottom sheet in the way
                    mEditLogTitle.setFocusableInTouchMode(true);
                    mEditLogContent.setFocusableInTouchMode(true);
                    mEditLogTitle.setFocusable(true);
                    mEditLogContent.setFocusable(true);
                } else {
                    // Don't allow editing covered text
                    mEditLogTitle.setFocusable(false);
                    mEditLogContent.setFocusable(false);
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        mEditTagsView.setTagsProvider(this);

        mEditTime.setOnClickListener(this);
        mEditTime.setOnLongClickListener(this);

        if (Settings.getBoolean(getActivity(), Settings.EASTEREGG)) {
            mEditLogTitle.setOnLongClickListener(mEastereggLongclickListener);
            mEditLogContent.setOnLongClickListener(mEastereggLongclickListener);
        }

        if (!restoreValues(savedInstanceState)) {
            loadContent();
            if (mEditItemId == -1) {
                // If we have an item id, initValues is called when we have the values for it
                initValues();
            }
        }

        if (savedInstanceState == null) {
            mAttachmentsFragment = new LogAttachmentsEditFragment();
            Bundle attachmentArgs = new Bundle();
            attachmentArgs.putLong(Constants.EXTRA_LOG_ITEM_ID, mEditItemId);
            mAttachmentsFragment.setArguments(attachmentArgs);
            getFragmentManager().beginTransaction()
                    .add(R.id.attachments_stub, mAttachmentsFragment, FRAGMENT_TAG_ATTACHMENTS)
                    .commit();
        } else {
            mAttachmentsFragment = (LogAttachmentsEditFragment) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAG_ATTACHMENTS);
        }

        return view;
    }

    private void initValues() {
        mEditLogTitle.setText(mInitTitle);
        mEditLogContent.setText(mInitContent);

        mTime = Calendar.getInstance();
        mTime.setTimeInMillis(mInitTime);

        mSetTags = (ArrayList<TagItem>) mInitTags.clone();

        updateTitle();
        mEditTagsView.updateContent();
    }

    private boolean restoreValues(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return false;
        }
        try {
            if (savedInstanceState.containsKey(KEY_INIT_TITLE)) {
                mInitTitle = savedInstanceState.getString(KEY_INIT_TITLE);
                mEditLogTitle.setText(savedInstanceState.getString(KEY_SET_TITLE));
                mInitContent = savedInstanceState.getString(KEY_INIT_CONTENT);
                mEditLogContent.setText(savedInstanceState.getString(KEY_SET_CONTENT));
                mInitTime = savedInstanceState.getLong(KEY_INIT_TIME);
                mTime = Calendar.getInstance();
                mTime.setTimeInMillis(savedInstanceState.getLong(KEY_SET_TIME));
                mInitTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_INIT_TAGS)));
                mSetTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_SET_TAGS)));
                mAvailableTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_AVAILABLE_TAGS)));

                updateTitle();
                mEditTagsView.updateContent();
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
        outState.putBoolean(KEY_TEMPORARY_EXISTENCE, mTemporaryExistence);
        outState.putString(KEY_INIT_TITLE, mInitTitle);
        outState.putString(KEY_SET_TITLE, mEditLogTitle.getText().toString());
        outState.putString(KEY_INIT_CONTENT, mInitContent);
        outState.putString(KEY_SET_CONTENT, mEditLogContent.getText().toString());
        outState.putLong(KEY_INIT_TIME, mInitTime);
        outState.putLong(KEY_SET_TIME, mTime.getTimeInMillis());
        outState.putParcelableArray(KEY_INIT_TAGS,
                mInitTags.toArray(new TagItem[mInitTags.size()]));
        outState.putParcelableArray(KEY_SET_TAGS,
                mSetTags.toArray(new TagItem[mSetTags.size()]));
        outState.putParcelableArray(KEY_AVAILABLE_TAGS,
                mAvailableTags.toArray(new TagItem[mAvailableTags.size()]));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(mAddItem
                ? R.menu.fragment_add_log_item
                : R.menu.fragment_edit_log_item, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveChanges();
                return true;
            case R.id.action_attachments:
                toggleAttachments();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mEditTime) {
            new DatePickerDialog(getActivity(), this, mTime.get(Calendar.YEAR),
                    mTime.get(Calendar.MONTH), mTime.get(Calendar.DAY_OF_MONTH)).show();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == mEditTime) {
            int[] viewPos = new int[2];
            view.getLocationInWindow(viewPos);
            int[] offsetPos = new int[2];
            view.getRootView().findViewById(android.R.id.content).getLocationInWindow(offsetPos);
            Toast toast = Toast.makeText(getActivity(), view.getContentDescription(),
                    Toast.LENGTH_SHORT);

            toast.setGravity(Gravity.END | Gravity.TOP, 0, viewPos[1] - offsetPos[1]
                    + view.getHeight());
            toast.show();
            return true;
        }
        return false;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        mTime.set(year, month, dayOfMonth);

        new TimePickerDialog(getActivity(), this, mTime.get(Calendar.HOUR_OF_DAY),
                mTime.get(Calendar.MINUTE), DateFormat.is24HourFormat(getActivity())).show();
        updateTitle();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mTime.set(Calendar.MINUTE, minute);
        updateTitle();
    }

    private void loadContent() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_LOAD);
    }

    private void loadContent(SQLiteDatabase db) {
        new LoadContentTask(db).execute();
    }

    private void saveChanges() {
        if (noChangesPresent()) {
            finish();
        } else {
            PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_SAVE);
        }
    }

    private void toggleAttachments() {
        if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            if (mAddItem) {
                PasswdHelper.getWritableDatabase(getActivity(), this,
                        DB_REQUEST_SAVE_FOR_ATTACHMENTS);
            } else {
                showAttachments();
            }
        } else {
            hideAttachments();
        }
    }

    private void showAttachments() {
        // Hide keyboard
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void hideAttachments() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void saveChanges(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Log.COLUMN_TITLE, mEditLogTitle.getText().toString());
        values.put(DbContract.Log.COLUMN_CONTENT, mEditLogContent.getText().toString());
        values.put(DbContract.Log.COLUMN_TIME, mTime.getTimeInMillis());
        values.put(DbContract.Log.COLUMN_TIME_END, System.currentTimeMillis());
        long id;
        if (mAddItem) {
            values.put(DbContract.Log._ID, LogItem.generateId());
            id = db.insert(DbContract.Log.TABLE, "null", values);
        } else {
            id = mEditItemId;
            String selection = DbContract.Log._ID + " = ?";
            String[] selectionArgs = {String.valueOf(id)};
            db.update(DbContract.Log.TABLE, values, selection, selectionArgs);
        }
        // Tags
        ArrayList<TagItem> addedTags = new ArrayList<>();
        ArrayList<TagItem> removedTags = new ArrayList<>();
        TagItem.checkTagListDiff(mInitTags, mSetTags, addedTags, removedTags);
        for (TagItem tag: addedTags) {
            ContentValues tagValues = new ContentValues();
            tagValues.put(DbContract.LogTags.COLUMN_LOG, id);
            tagValues.put(DbContract.LogTags.COLUMN_TAG, tag.id);
            db.insert(DbContract.LogTags.TABLE, "null", tagValues);
        }
        if (!removedTags.isEmpty()) {
            String selection = DbContract.LogTags.COLUMN_LOG + " = ? AND ("
                    + DbContract.LogTags.COLUMN_TAG + " = ?";
            String[] selectionArgs = new String[removedTags.size()+1];
            selectionArgs[0] = String.valueOf(id);
            selectionArgs[1] = String.valueOf(removedTags.get(0).id);
            for (int i = 1; i < removedTags.size(); i++) {
                selection += " OR " + DbContract.LogTags.COLUMN_TAG + " = ?";
                selectionArgs[i+1] = String.valueOf(removedTags.get(i).id);
            }
            selection += ")";
            db.delete(DbContract.LogTags.TABLE, selection, selectionArgs);
        }
        db.close();
        finish();
        // Notify about added/edited item
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent(Constants.EVENT_LOG_UPDATE).putExtra(Constants.EXTRA_LOG_ITEM_ID, id)
        );
    }

    /**
     * Attachments need to be saved immediately, so log entry needs to already exist.
     * In case we're in add mode, add an empty entry and convert to edit mode
     */
    private void ensureExistence(SQLiteDatabase db) {
        if (mAddItem) {
            ContentValues values = new ContentValues();
            values.put(DbContract.Log._ID, LogItem.generateId());
            values.put(DbContract.Log.COLUMN_TITLE, "");
            values.put(DbContract.Log.COLUMN_CONTENT, "");
            values.put(DbContract.Log.COLUMN_TIME, mTime.getTimeInMillis());
            values.put(DbContract.Log.COLUMN_TIME_END, System.currentTimeMillis());
            mEditItemId = db.insert(DbContract.Log.TABLE, "null", values);
            db.close();
            mAddItem = false;
            mTemporaryExistence = true;
            mAttachmentsFragment.setLogId(mEditItemId);
            // Notify about added/edited item
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(
                    Constants.EVENT_LOG_UPDATE).putExtra(Constants.EXTRA_LOG_ITEM_ID, mEditItemId));
            // TODO invalidate options menu? currenlty both are the same either way though
        } else {
            db.close();
        }
    }

    private void discardExistence() {
        if (mTemporaryExistence && noChangesPresent() && !mAttachmentsFragment.hasAttachments()) {
            PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_DELETE);
        } else {
            finish();
        }
    }

    private void deleteEntry(SQLiteDatabase db) {
        String selection = DbContract.Log._ID + " = ?";
        String[] selectionArgs = {String.valueOf(mEditItemId)};
        db.delete(DbContract.Log.TABLE, selection, selectionArgs);
        db.close();
        finish();
        // Notify about deleted item
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent(Constants.EVENT_LOG_UPDATE)
                        .putExtra(Constants.EXTRA_LOG_ITEM_ID, mEditItemId)
        );
        finish();
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
            case DB_REQUEST_SAVE_FOR_ATTACHMENTS:
                ensureExistence(db);
                showAttachments();
                break;
            case DB_REQUEST_DELETE:
                deleteEntry(db);
                break;
        }
    }

    private void updateTitle() {
        if (!isAdded()) {
            return;
        }
        getActivity().setTitle(getString(mAddItem
                        ? R.string.title_log_item_add
                        : R.string.title_log_item_edit,
                DateFormatter.getDateForTitle(getActivity(), mTime.getTimeInMillis())));
    }

    @Override
    public boolean onUpOrBackPressed(boolean backPress) {
        if (backPress &&
                mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (noChangesPresent()) {
            discardExistence();
        } else {
            // User made some changes; make sure he gets what he wants after exiting the screen
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_unsaved_changes)
                    .setPositiveButton(R.string.dialog_save_changes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    saveChanges();
                                }
                            })
                    .setNegativeButton(R.string.dialog_discard_changes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            })
                    .setNeutralButton(R.string.dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Only close dialog
                                }
                            })
                    .show();
        }
        return true;
    }

    private boolean noChangesPresent() {
        return mInitTitle.equals(mEditLogTitle.getText().toString())
                && mInitContent.equals(mEditLogContent.getText().toString())
                && mInitTime == mTime.getTimeInMillis()
                && TagItem.checkTagListDiff(mInitTags, mSetTags, null, null);
    }

    private void finish() {
        getActivity().finish();
    }

    private class LoadContentTask extends LoadLogItemsTask {

        LoadContentTask(SQLiteDatabase db) {
            super(db);
        }

        @Override
        protected ArrayList<LogItem> doInBackground(Void... params) {
            // Request available tags
            mAvailableTags = LoadTagItemsTask.loadAvailableTags(mDb, null);
            if (mAddItem) {
                // We're done
                mDb.close();
                return new ArrayList<>();
            } else {
                // Request log item
                return super.doInBackground(params);
            }
        }

        @Override
        protected void onPostExecute(ArrayList<LogItem> result) {
            if (result.isEmpty()) {
                if (!mAddItem) {
                    Log.e(TAG, "DB response is empty");
                    finish();
                } // else: we're done
            } else if (result.size() > 1) {
                Log.e(TAG, "Too many objects found");
                finish();
            } else {
                LogItem logItem = result.get(0);
                mInitTime = logItem.time;
                mInitTitle = logItem.title;
                mInitContent = logItem.content;
                mInitTags = logItem.tags;
                initValues();
            }
        }

        @Override
        protected String getSelection() {
            return DbContract.Log._ID + " = " + mEditItemId;
        }
    }

    @Override
    public List<TagItem> getAvailableTags() {
        return mAvailableTags;
    }

    @Override
    public List<TagItem> getSetTags() {
        return mSetTags;
    }

    @Override
    public void onDeleteTag(TagItem item) {
        mInitTags.remove(item);
    }

    @Override
    public void onSetTagsChanged() {}

    private View.OnLongClickListener mEastereggLongclickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            if (view instanceof EditText) {
                ((EditText) view).setText(((EditText) view).getText().toString() +
                        getString(R.string.easteregg));
                return true;
            }
            return false;
        }
    };
}
