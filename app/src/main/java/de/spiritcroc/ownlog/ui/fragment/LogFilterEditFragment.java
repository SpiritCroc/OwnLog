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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.data.DbContract;
import de.spiritcroc.ownlog.data.LoadLogFiltersTask;
import de.spiritcroc.ownlog.data.LoadTagItemsTask;
import de.spiritcroc.ownlog.data.LogFilter;
import de.spiritcroc.ownlog.data.TagItem;
import de.spiritcroc.ownlog.ui.view.EditTagsView;

public class LogFilterEditFragment extends DialogFragment
        implements PasswdHelper.RequestDbListener {

    private static final String TAG = LogFilterEditFragment.class.getSimpleName();

    // Saved instance state bundle keys
    private static final String KEY_ADD_ITEM =
            LogFilterEditFragment.class.getName() + ".add_item";
    private static final String KEY_EDIT_ITEM_ID =
            LogFilterEditFragment.class.getName() + ".edit_item_id";
    private static final String KEY_INIT_NAME =
            LogFilterEditFragment.class.getName() + ".init_name";
    private static final String KEY_SET_NAME =
            LogFilterEditFragment.class.getName() + ".set_name";
    private static final String KEY_INIT_SORT_ORDER =
            LogFilterEditFragment.class.getName() + ".init_sort_order";
    private static final String KEY_SORT_ORDER =
            LogFilterEditFragment.class.getName() + ".sort_order";
    private static final String KEY_INIT_STRICT_TAG_FILTER =
            LogFilterEditFragment.class.getName() + ".init_strict_filter_tags";
    private static final String KEY_SET_STRICT_TAG_FILTER =
            LogFilterEditFragment.class.getName() + ".set_strict_filter_tags";
    private static final String KEY_INIT_TAGS =
            LogItemEditFragment.class.getName() + ".init_tags";
    private static final String KEY_INIT_EXCLUDED_TAGS =
            LogItemEditFragment.class.getName() + ".init_excluded_tags";
    private static final String KEY_SET_TAGS =
            LogItemEditFragment.class.getName() + ".set_tags";
    private static final String KEY_EXCLUDED_TAGS =
            LogItemEditFragment.class.getName() + ".excluded_tags";
    private static final String KEY_AVAILABLE_TAGS =
            LogItemEditFragment.class.getName() + ".available_tags";

    private static final int DB_REQUEST_LOAD = 1;
    private static final int DB_REQUEST_SAVE = 2;
    private static final int DB_REQUEST_DELETE = 3;

    private boolean mAddItem = true;
    private long mEditItemId = -1;

    private String mInitName;
    private String mInitSortOrder = "";
    private int mSortOrder;
    private boolean mInitStrictFilterTags = false;
    private ArrayList<TagItem> mInitTags = new ArrayList<>();
    private ArrayList<TagItem> mInitExcludedTags = new ArrayList<>();
    private ArrayList<TagItem> mAvailableTags = new ArrayList<>();
    private ArrayList<TagItem> mSetTags = new ArrayList<>();
    private ArrayList<TagItem> mExcludedTags = new ArrayList<>();
    private String[] mSortOrderValues;

    private EditText mEditName;
    private Spinner mSpinSortOrder;
    private CheckBox mCheckStrictFilterTags;
    private View mStrictFilterTagsInfoNoTags;
    private View mStrictFilterTagsInfoTags;
    private EditTagsView mEditTagsView;
    private EditTagsView mEditExcludedTagsView;

    public LogFilterEditFragment setEditItemId(long id) {
        mAddItem = false;
        mEditItemId = id;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view =
                activity.getLayoutInflater().inflate(R.layout.log_filter_edit_item, null);

        mEditName = (EditText) view.findViewById(R.id.name_edit);
        mSpinSortOrder = (Spinner) view.findViewById(R.id.sort_order_spin);
        mCheckStrictFilterTags = (CheckBox) view.findViewById(R.id.tags_strict_check);
        mStrictFilterTagsInfoNoTags = view.findViewById(R.id.text_view_tags_strict_no_tags);
        mStrictFilterTagsInfoTags = view.findViewById(R.id.text_view_tags_strict_tags);
        mEditTagsView = (EditTagsView) view.findViewById(R.id.edit_tags_view);
        mEditTagsView.setTagsProvider(mTagsProvider);
        mEditTagsView.setAvailableTagsFilter(mAvailableTagsFilter);
        mEditExcludedTagsView = (EditTagsView) view.findViewById(R.id.edit_excluded_tags_view);
        mEditExcludedTagsView.setTagsProvider(mExcludedTagsProvider);
        mEditExcludedTagsView.setAvailableTagsFilter(mAvailableTagsFilter);

        View.OnClickListener strictFilterInfoClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCheckStrictFilterTags.toggle();
            }
        };
        mStrictFilterTagsInfoNoTags.setOnClickListener(strictFilterInfoClickListener);
        mStrictFilterTagsInfoTags.setOnClickListener(strictFilterInfoClickListener);

        mSortOrderValues = getResources().getStringArray(R.array.edit_log_filter_sort_order_values);
        mSpinSortOrder.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.edit_log_filter_sort_order_entries,
                R.layout.support_simple_spinner_dropdown_item));
        mSpinSortOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSortOrder = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        mInitName = getString(R.string.log_filter_default_new_name);
        mInitSortOrder = mSortOrderValues[0];

        boolean restoredValues = restoreValues(savedInstanceState);

        builder.setTitle(mAddItem ? R.string.title_log_filter_add : R.string.title_log_filter_edit)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only dismiss
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
            loadContent();
            if (mAddItem) {
                initValues(alertDialog);
            }
        }

        return alertDialog;
    }

    private void initValues(Dialog dialog) {
        mEditName.setText(mInitName);
        if (mInitName.equals(getString(R.string.log_filter_default_new_name))) {
            // It's unlikely that the user wants to keep the default "new filter" name
            mEditName.selectAll();
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        mSortOrder = Arrays.asList(mSortOrderValues).indexOf(mInitSortOrder);
        if (mSortOrder < 0) {
            // Select default sort order
            mSortOrder = 0;
        }
        mSpinSortOrder.setSelection(mSortOrder);

        mCheckStrictFilterTags.setChecked(mInitStrictFilterTags);

        mSetTags = (ArrayList<TagItem>) mInitTags.clone();
        mExcludedTags = (ArrayList<TagItem>) mInitExcludedTags.clone();
        mEditTagsView.updateContent();
        mEditExcludedTagsView.updateContent();

        updateStrictFilterTagsInfo();
    }

    private void updateStrictFilterTagsInfo() {
        if (mSetTags.isEmpty()) {
            mStrictFilterTagsInfoNoTags.setVisibility(View.VISIBLE);
            mStrictFilterTagsInfoTags.setVisibility(View.GONE);
        } else {
            mStrictFilterTagsInfoNoTags.setVisibility(View.GONE);
            mStrictFilterTagsInfoTags.setVisibility(View.VISIBLE);
        }
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
                mEditName.setText(savedInstanceState.getString(KEY_SET_NAME));
                mInitStrictFilterTags = savedInstanceState.getBoolean(KEY_INIT_STRICT_TAG_FILTER);
                mCheckStrictFilterTags.setChecked(
                        savedInstanceState.getBoolean(KEY_SET_STRICT_TAG_FILTER));

                mInitSortOrder = savedInstanceState.getString(KEY_INIT_SORT_ORDER);
                mSortOrder = savedInstanceState.getInt(KEY_SORT_ORDER);
                mInitTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_INIT_TAGS)));
                mInitExcludedTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_INIT_EXCLUDED_TAGS)));
                mSetTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_SET_TAGS)));
                mExcludedTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_EXCLUDED_TAGS)));
                mAvailableTags = new ArrayList<>(Arrays.asList(
                        (TagItem[]) savedInstanceState.getParcelableArray(KEY_AVAILABLE_TAGS)));

                mSortOrder = Arrays.asList(mSortOrderValues).indexOf(mInitSortOrder);
                if (mSortOrder < 0) {
                    // Select default sort order
                    mSortOrder = 0;
                }
                mSpinSortOrder.setSelection(mSortOrder);

                mEditTagsView.updateContent();
                mEditExcludedTagsView.updateContent();
                updateStrictFilterTagsInfo();
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
        outState.putString(KEY_SET_NAME, mEditName.getText().toString());
        outState.putString(KEY_INIT_SORT_ORDER,mInitSortOrder);
        outState.putInt(KEY_SORT_ORDER, mSortOrder);
        outState.putBoolean(KEY_INIT_STRICT_TAG_FILTER, mInitStrictFilterTags);
        outState.putBoolean(KEY_SET_STRICT_TAG_FILTER, mCheckStrictFilterTags.isChecked());
        outState.putParcelableArray(KEY_INIT_TAGS,
                mInitTags.toArray(new TagItem[mInitTags.size()]));
        outState.putParcelableArray(KEY_INIT_EXCLUDED_TAGS,
                mInitExcludedTags.toArray(new TagItem[mInitExcludedTags.size()]));
        outState.putParcelableArray(KEY_SET_TAGS,
                mSetTags.toArray(new TagItem[mSetTags.size()]));
        outState.putParcelableArray(KEY_EXCLUDED_TAGS,
                mExcludedTags.toArray(new TagItem[mExcludedTags.size()]));
        outState.putParcelableArray(KEY_AVAILABLE_TAGS,
                mAvailableTags.toArray(new TagItem[mAvailableTags.size()]));
    }

    private void loadContent() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_LOAD);
    }

    private void loadContent(SQLiteDatabase db) {
        new LoadContentTask(db, getActivity()).execute();
    }



    private class LoadContentTask extends LoadLogFiltersTask {

        LoadContentTask(SQLiteDatabase db, Context context) {
            super(db, context);
        }

        @Override
        protected ArrayList<LogFilter> doInBackground(Void... params) {
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
        protected void onPostExecute(ArrayList<LogFilter> result) {
            if (result.isEmpty()) {
                if (!mAddItem) {
                    Log.e(TAG, "DB response is empty for id " + mEditItemId);
                    dismiss();
                } // else: we're done
            } else if (result.size() > 1) {
                Log.e(TAG, "Too many objects found for id " + mEditItemId);
                for (LogFilter filter: result) {
                    Log.e(TAG, "" + filter);
                }
                dismiss();
            } else {
                LogFilter logFilter = result.get(0);
                mInitName = logFilter.name;
                mInitSortOrder = logFilter.sortOrder;
                mInitStrictFilterTags = logFilter.strictFilterTags;
                mInitTags = logFilter.filterTagsList;
                mInitExcludedTags = logFilter.filterExcludedTagsList;
                initValues(getDialog());
            }
        }

        @Override
        protected String getSelection() {
            return DbContract.LogFilter._ID + " = " + mEditItemId;
        }
    }

    private boolean noChangesPresent() {
        return mInitName.equals(mEditName.getText().toString())
                && mInitSortOrder.equals(mSortOrderValues[mSortOrder])
                && mInitStrictFilterTags == mCheckStrictFilterTags.isChecked()
                && TagItem.checkTagListDiff(mInitTags, mSetTags, null, null)
                && TagItem.checkTagListDiff(mInitExcludedTags, mExcludedTags, null, null);
    }

    private void saveChanges() {
        if (noChangesPresent()) {
            finish();
        } else {
            PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_SAVE);
        }
    }

    private void saveChanges(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(DbContract.LogFilter.COLUMN_NAME, mEditName.getText().toString());
        values.put(DbContract.LogFilter.COLUMN_SORT_ORDER, mSortOrderValues[mSortOrder]);
        values.put(DbContract.LogFilter.COLUMN_STRICT_FILTER_TAGS,
                mCheckStrictFilterTags.isChecked());
        long id;
        if (mAddItem) {
            values.put(DbContract.LogFilter._ID, LogFilter.generateId());
            id = db.insert(DbContract.LogFilter.TABLE, "null", values);
        } else {
            id = mEditItemId;
            String selection = DbContract.LogFilter._ID + " = ?";
            String[] selectionArgs = {String.valueOf(id)};
            db.update(DbContract.LogFilter.TABLE, values, selection, selectionArgs);
        }
        // Tags
        ArrayList<TagItem> addedTags = new ArrayList<>();
        ArrayList<TagItem> removedTags = new ArrayList<>();
        ArrayList<TagItem> addedExcludedTags = new ArrayList<>();
        ArrayList<TagItem> removedExcludedTags = new ArrayList<>();
        TagItem.checkTagListDiff(mInitTags, mSetTags, addedTags, removedTags);
        TagItem.checkTagListDiff(mInitExcludedTags, mExcludedTags,
                addedExcludedTags, removedExcludedTags);
        // First remove tags to avoid illegal states
        removeTags(db, id, removedTags);
        removeTags(db, id, removedExcludedTags);
        addTags(db, id, addedTags, false);
        addTags(db, id, addedExcludedTags, true);
        db.close();
        finish();
        // Notify about changes
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent(Constants.EVENT_LOG_UPDATE)
                        .putExtra(Constants.EXTRA_LOG_FILTER_ITEM_ID, id)
        );
    }

    private void removeTags(SQLiteDatabase db, long id, ArrayList<TagItem> removedTags) {
        if (!removedTags.isEmpty()) {
            String selection = DbContract.LogFilter_Tags.COLUMN_FILTER + " = ? AND ("
                    + DbContract.LogFilter_Tags.COLUMN_TAG + " = ?";
            String[] selectionArgs = new String[removedTags.size()+1];
            selectionArgs[0] = String.valueOf(id);
            selectionArgs[1] = String.valueOf(removedTags.get(0).id);
            for (int i = 1; i < removedTags.size(); i++) {
                selection += " OR " + DbContract.LogFilter_Tags.COLUMN_TAG + " = ?";
                selectionArgs[i+1] = String.valueOf(removedTags.get(i).id);
            }
            selection += ")";
            db.delete(DbContract.LogFilter_Tags.TABLE, selection, selectionArgs);
        }
    }

    private void addTags(SQLiteDatabase db, long id, ArrayList<TagItem> addedTags,
                         boolean excluded) {
        for (TagItem tag: addedTags) {
            ContentValues tagValues = new ContentValues();
            tagValues.put(DbContract.LogFilter_Tags.COLUMN_FILTER, id);
            tagValues.put(DbContract.LogFilter_Tags.COLUMN_TAG, tag.id);
            tagValues.put(DbContract.LogFilter_Tags.COLUMN_EXCLUDE_TAG, excluded);
            db.insert(DbContract.LogFilter_Tags.TABLE, "null", tagValues);
        }
    }

    private void promptDelete() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dialog_delete_log_filter_entry_title, mInitName))
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
        String selection = DbContract.LogFilter._ID + " = ?";
        String[] selectionArgs = {String.valueOf(mEditItemId)};
        db.delete(DbContract.LogFilter.TABLE, selection, selectionArgs);
        db.close();
        dismiss();
        // Notify about changes
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent(Constants.EVENT_LOG_UPDATE)
                        .putExtra(Constants.EXTRA_LOG_FILTER_ITEM_ID, mEditItemId)
        );
        Toast.makeText(getActivity(),
                // Should be impossible to create/edit a tag with empty name, so we can assume
                // here mInitName is not empty
                getString(R.string.dialog_deleted_log_filter_entry_toast_title, mInitName),
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

    private EditTagsView.EditTagsProvider mTagsProvider = new EditTagsView.EditTagsProvider() {
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
        public void onSetTagsChanged() {
            updateStrictFilterTagsInfo();
            //mEditExcludedTagsView.updateContent();
        }

        @Override
        public Activity getActivity() {
            return LogFilterEditFragment.this.getActivity();
        }
    };

    private EditTagsView.EditTagsProvider mExcludedTagsProvider =
            new EditTagsView.EditTagsProvider() {
                @Override
                public List<TagItem> getAvailableTags() {
                    return mAvailableTags;
                }

                @Override
                public List<TagItem> getSetTags() {
                    return mExcludedTags;
                }

                @Override
                public void onDeleteTag(TagItem item) {
                    mInitTags.remove(item);
                }

                @Override
                public void onSetTagsChanged() {
                    //mEditTagsView.updateContent();
                }

                @Override
                public Activity getActivity() {
                    return LogFilterEditFragment.this.getActivity();
                }
    };

    private EditTagsView.AvailableTagsFilter mAvailableTagsFilter =
            new EditTagsView.AvailableTagsFilter() {
                @Override
                public boolean shouldShowTag(TagItem tagItem) {
                    return !mSetTags.contains(tagItem) && !mExcludedTags.contains(tagItem);
                }
    };

    private void finish() {
        dismiss();
    }
}
