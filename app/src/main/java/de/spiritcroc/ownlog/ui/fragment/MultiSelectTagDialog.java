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
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.buildware.widget.indeterm.IndeterminateCheckBox;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.data.DbContract;
import de.spiritcroc.ownlog.data.LoadTagItemsTask;
import de.spiritcroc.ownlog.data.LogItem;
import de.spiritcroc.ownlog.data.TagItem;

public class MultiSelectTagDialog extends DialogFragment implements PasswdHelper.RequestDbListener {

    private static final String TAG = MultiSelectTagDialog.class.getSimpleName();

    // Saved instance state bundle keys
    private static final String KEY_LOG_ITEMS =
            MultiSelectTagDialog.class.getName() + ".log_items";

    private static final int DB_REQUEST_LOAD = 1;
    private static final int DB_REQUEST_SAVE = 2;

    private LogItem[] mEditItems;

    private ListView mTagListView;

    private ArrayList<TagItem> mAvailableTagItems;
    private ArrayList<Boolean> mTagSelection;
    private ArrayList<Boolean> mTagInitSelection = new ArrayList<>();

    private BroadcastReceiver mTagBroadcastReceiver;

    /**
     * @param editItems
     * LogItems to edit, current tags need to be loaded!
     *
     */
    public MultiSelectTagDialog setEditItems(LogItem... editItems) {
        mEditItems = editItems;
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(
                        mTagBroadcastReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                Parcelable item =
                                        intent.getParcelableExtra(Constants.EXTRA_TAG_ITEM);
                                if (!(item instanceof TagItem)) {
                                    Log.w(TAG, "Received invalid tag item: " + item);
                                    return;
                                }
                                switch (intent.getIntExtra(Constants.EXTRA_TAG_ACTION, -1)) {
                                    case Constants.TAG_ACTION_ADD:
                                        onAddTagResult((TagItem) item);
                                        break;
                                    case Constants.TAG_ACTION_EDIT:
                                        onEditTagResult((TagItem) item);
                                        break;
                                    case Constants.TAG_ACTION_DELETE:
                                        onDeleteTagResult((TagItem) item);
                                        break;
                                    default:
                                        Log.w(TAG, "Received unknown tag action: " +
                                                intent.getIntExtra(Constants.EXTRA_TAG_ACTION, -1));
                                        break;
                                }
                            }
                        },
                        new IntentFilter(Constants.EVENT_TAG_UPDATE)
                );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mTagBroadcastReceiver);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = createView();

        restoreValues(savedInstanceState);
        loadContent();

        final AlertDialog alertDialog = builder.setTitle(R.string.dialog_select_tags)
                .setView(view)
                .setPositiveButton(R.string.dialog_save_changes, null)
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Only dismiss
                            }
                        })
                .setNeutralButton(R.string.dialog_add, null)
                .create();

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
                alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new TagItemEditFragment()
                                        .setShouldHideKeyboardOnDismiss(true)
                                        .show(getFragmentManager(), "TagItemEditFragment");
                            }
                        });
            }
        });

        return alertDialog;
    }

    private View createView() {
        mTagListView = new ListView(getActivity());
        return mTagListView;
    }

    private boolean restoreValues(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return false;
        }
        try {
            if (savedInstanceState.containsKey(KEY_LOG_ITEMS)) {
                mEditItems = (LogItem[]) savedInstanceState.getParcelableArray(KEY_LOG_ITEMS);
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
        outState.putParcelableArray(KEY_LOG_ITEMS, mEditItems);
    }

    private View.OnClickListener mTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TagItemHolder holder = (TagItemHolder) ((View) view.getParent()).getTag();
            holder.checkBox.toggle();
        }
    };

    private IndeterminateCheckBox.OnStateChangedListener mToggleListener =
            new IndeterminateCheckBox.OnStateChangedListener() {
                @Override
                public void onStateChanged(IndeterminateCheckBox indeterminateCheckBox,
                                           @Nullable Boolean aBoolean) {
                    TagItemHolder holder =
                            (TagItemHolder) ((View) indeterminateCheckBox.getParent()).getTag();
                    mTagSelection.set(holder.pos, holder.checkBox.getState());
                }
            };

    private View.OnLongClickListener mTagLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            TagItemHolder holder = (TagItemHolder) ((View) view.getParent()).getTag();
            Boolean initValue = mTagInitSelection.get(holder.pos);
            holder.checkBox.setState(initValue);
            mTagSelection.set(holder.pos, initValue);
            return true;
        }
    };

    private View.OnClickListener mTagEditClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TagItemHolder holder = (TagItemHolder) ((View) view.getParent()).getTag();
            new TagItemEditFragment()
                    .setEditItemId(mAvailableTagItems.get(holder.pos).id)
                    .setShouldHideKeyboardOnDismiss(true)
                    .show(getFragmentManager(), "TagItemEditFragment");
        }
    };

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        switch (requestId) {
            case DB_REQUEST_LOAD:
                loadContent(db);
                break;
            case DB_REQUEST_SAVE:
                saveChanges(db);
                break;
        }
    }

    private void loadContent() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_LOAD);
    }

    private void loadContent(SQLiteDatabase db) {
        new LoadContentTask(db).execute();
    }

    private class LoadContentTask extends LoadTagItemsTask {

        LoadContentTask(SQLiteDatabase db) {
            super(db);
        }

        @Override
        protected void onPostExecute(ArrayList<TagItem> result) {
            mAvailableTagItems = result;
            mTagInitSelection.clear();
            for (TagItem tag: mAvailableTagItems) {
                boolean alwaysSet = true;
                boolean setAtLeastOnce = false;

                for (LogItem log: mEditItems) {
                    if (log.tags.contains(tag)) {
                        setAtLeastOnce = true;
                    } else {
                        alwaysSet = false;
                    }
                    if (setAtLeastOnce && !alwaysSet) {
                        // Result is already known
                        break;
                    }
                }

                mTagInitSelection.add(setAtLeastOnce ? (alwaysSet ? true : null) : (Boolean) false);
            }
            mTagSelection = (ArrayList<Boolean>) mTagInitSelection.clone();
            updateList();
        }
    }

    private void updateList() {
        mTagListView.setAdapter(new TagArrayAdapter(getActivity(),
                R.layout.log_tag_batch_edit_item,
                mAvailableTagItems.toArray(new TagItem[mAvailableTagItems.size()])));
    }

    private void saveChanges() {
        if (noChangesPresent()) {
            dismiss();
        } else {
            PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_SAVE);
        }
    }

    private void saveChanges(SQLiteDatabase db) {
        ArrayList<TagItem> addedTags = new ArrayList<>();
        ArrayList<TagItem> removedTags = new ArrayList<>();
        for (int i = 0; i < mTagSelection.size(); i++) {
            if (selectionChangedAt(i)) {
                Boolean selection = mTagSelection.get(i);
                if (Boolean.TRUE.equals(selection)) {
                    addedTags.add(mAvailableTagItems.get(i));
                } else if (Boolean.FALSE.equals(selection)) {
                    removedTags.add(mAvailableTagItems.get(i));
                }
            }
        }
        for (LogItem logItem: mEditItems) {
            for (TagItem tag : addedTags) {
                ContentValues tagValues = new ContentValues();
                tagValues.put(DbContract.LogTags.COLUMN_LOG, logItem.id);
                tagValues.put(DbContract.LogTags.COLUMN_TAG, tag.id);
                db.insert(DbContract.LogTags.TABLE, "null", tagValues);
            }
        }
        if (!removedTags.isEmpty()) {
            String selection = "(" + DbContract.LogTags.COLUMN_LOG + " = ?";
            String[] selectionArgs = new String[removedTags.size() + mEditItems.length];
            int index = 0;
            selectionArgs[index++] = String.valueOf(mEditItems[0].id);
            for (int i = 1; i < mEditItems.length; i++) {
                selection += " OR " + DbContract.LogTags.COLUMN_LOG + " = ?";
                selectionArgs[index++] = String.valueOf(mEditItems[i].id);
            }
            selection += ") AND ("+ DbContract.LogTags.COLUMN_TAG + " = ?";
            selectionArgs[index++] = String.valueOf(removedTags.get(0).id);
            for (int i = 1; i < removedTags.size(); i++) {
                selection += " OR " + DbContract.LogTags.COLUMN_TAG + " = ?";
                selectionArgs[index++] = String.valueOf(removedTags.get(i).id);
            }
            selection += ")";
            db.delete(DbContract.LogTags.TABLE, selection, selectionArgs);
        }
        db.close();
        dismiss();
        // Notify about update
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent(Constants.EVENT_LOG_UPDATE));
    }

    private boolean noChangesPresent() {
        for (int i = 0; i < mTagInitSelection.size(); i++) {
            if (selectionChangedAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean selectionChangedAt(int position) {
        Boolean init = mTagInitSelection.get(position);
        Boolean current = mTagSelection.get(position);
        return ((init == null) != (current == null))
                || (init != null && !init.equals(current));
    }

    public void onEditTagResult(TagItem item) {
        int index = mAvailableTagItems.indexOf(item);
        if (index >= 0) {
            mAvailableTagItems.set(index, item);
        }
        updateList();
    }

    public void onAddTagResult(TagItem item) {
        mAvailableTagItems.add(item);
        mTagSelection.add(true);
        mTagInitSelection.add(false);
        updateList();
    }

    public void onDeleteTagResult(TagItem item) {
        int index = mAvailableTagItems.indexOf(item);
        if (index >= 0) {
            mAvailableTagItems.remove(index);
            mTagSelection.remove(index);
            mTagInitSelection.remove(index);
        }
        updateList();
    }

    private class TagArrayAdapter extends ArrayAdapter<TagItem> {

        public TagArrayAdapter(Context context, int resource, TagItem[] objects) {
            super(context, resource, objects);
        }

        @Override
        public @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TagItemHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.log_tag_batch_edit_item, parent, false);

                holder = new TagItemHolder();
                holder.name = (TextView) convertView.findViewById(R.id.tag_name);
                holder.checkBox =
                        (IndeterminateCheckBox) convertView.findViewById(R.id.tag_checkbox);

                convertView.findViewById(R.id.tag_edit).setOnClickListener(mTagEditClickListener);
                holder.name.setOnClickListener(mTagClickListener);
                holder.name.setOnLongClickListener(mTagLongClickListener);
                holder.checkBox.setOnStateChangedListener(mToggleListener);
                holder.checkBox.setOnLongClickListener(mTagLongClickListener);

                convertView.setTag(holder);
            } else {
                holder = (TagItemHolder) convertView.getTag();
            }

            TagItem item = getItem(position);
            holder.pos = position;
            holder.name.setText(item.name);
            holder.checkBox.setState(mTagSelection.get(position));

            return convertView;
        }
    }

    private static class TagItemHolder {
        int pos;
        TextView name;
        IndeterminateCheckBox checkBox;
    }
}
