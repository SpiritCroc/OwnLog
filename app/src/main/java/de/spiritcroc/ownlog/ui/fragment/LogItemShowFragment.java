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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.DateFormatter;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.data.DbContract;
import de.spiritcroc.ownlog.data.LoadLogItemsTask;
import de.spiritcroc.ownlog.data.LogItem;
import de.spiritcroc.ownlog.data.TagItem;
import de.spiritcroc.ownlog.ui.activity.SingleFragmentActivity;

public class LogItemShowFragment extends BaseFragment implements PasswdHelper.RequestDbListener {

    private static final String TAG = LogItemShowFragment.class.getSimpleName();

    private static final int DB_REQUEST_LOAD = 1;
    private static final int DB_REQUEST_DELETE = 2;

    private long mItemId = -1;

    private TextView mTitleView;
    private TextView mContentView;
    private TextView mTagsView;

    public static void show(Context context, @NonNull LogItem logItem) {
        Intent intent = new Intent(context, SingleFragmentActivity.class)
                .putExtra(Constants.EXTRA_FRAGMENT_CLASS,
                        LogItemShowFragment.class.getName());
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putLong(Constants.EXTRA_LOG_ITEM_ID, logItem.id);
        intent.putExtra(Constants.EXTRA_FRAGMENT_BUNDLE, fragmentArgs);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() == null) {
            Log.e(TAG, "onCreate: getArguments() == null");
            finish();
        } else {
            mItemId = getArguments().getLong(Constants.EXTRA_LOG_ITEM_ID, -1);
            if (mItemId == -1) {
                Log.e(TAG, "onCreate: could not find out id of item to display");
                finish();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.log_show_item, container, false);
        mTitleView = (TextView) v.findViewById(R.id.title_view);
        mContentView = (TextView) v.findViewById(R.id.content_view);
        mTagsView = (TextView) v.findViewById(R.id.tags_view);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadContent();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_show_log_item, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                LogItemEditFragment.show(getActivity(), new LogItem(mItemId));
                return true;
            case R.id.action_delete:
                promptDelete();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        switch (requestId) {
            case DB_REQUEST_LOAD:
                loadContent(db);
                break;
            case DB_REQUEST_DELETE:
                deleteEntry(db);
                break;
            default:
                Log.e(TAG, "receiveWritableDatabase: unknwon requestId " + requestId);
        }
    }

    private void loadContent() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_LOAD);
    }

    private void loadContent(SQLiteDatabase db) {
        new LoadContentTask(db).execute();
    }

    private void updateViews(LogItem logItem) {
        if (TextUtils.isEmpty(logItem.title)) {
            mTitleView.setVisibility(View.GONE);
        } else {
            mTitleView.setVisibility(View.VISIBLE);
            mTitleView.setText(logItem.title);
        }

        if (TextUtils.isEmpty(logItem.content)) {
            mContentView.setVisibility(View.GONE);
        } else {
            mContentView.setVisibility(View.VISIBLE);
            mContentView.setText(logItem.content);
        }

        String tags = formatTags(logItem.tags);
        if (TextUtils.isEmpty(tags)) {
            mTagsView.setVisibility(View.GONE);
        } else {
            mTagsView.setVisibility(View.VISIBLE);
            mTagsView.setText(tags);
        }

        getActivity().setTitle(getString(R.string.title_log_item_show,
                DateFormatter.getFullDateTime(getActivity(), logItem.time)));
    }

    private String formatTags(ArrayList<TagItem> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        String result = tags.get(0).name;
        for (int i = 1; i < tags.size(); i++) {
            result += getString(R.string.log_list_tag_list_separator) + tags.get(1).name;
        }
        return result;
    }

    private class LoadContentTask extends LoadLogItemsTask {

        LoadContentTask(SQLiteDatabase db) {
            super(db);
        }

        @Override
        protected void onPostExecute(ArrayList<LogItem> result) {
            if (result.isEmpty()) {
                Log.e(TAG, "DB response is empty");
                finish();
            } else if (result.size() > 1) {
                Log.e(TAG, "Too many objects found");
                finish();
            } else {
                updateViews(result.get(0));
            }
        }

        @Override
        protected String getSelection() {
            return DbContract.Log._ID + " = " + mItemId;
        }
    }

    private void promptDelete() {
        String logItemTitle = mTitleView.getText().toString();
        new AlertDialog.Builder(getActivity())
                .setMessage(TextUtils.isEmpty(logItemTitle)
                        ? getString(R.string.dialog_delete_log_entry)
                        : getString(R.string.dialog_delete_log_entry_title, logItemTitle))
                .setPositiveButton(R.string.dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteEntry();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Only close dialog
                            }
                        })
                .show();
    }

    private void deleteEntry() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_DELETE);
    }

    private void deleteEntry(SQLiteDatabase db) {
        String selection = DbContract.Log._ID + " = ?";
        String[] selectionArgs = {String.valueOf(mItemId)};
        db.delete(DbContract.Log.TABLE, selection, selectionArgs);
        finish();
        // Notify about deleted item
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                new Intent(Constants.EVENT_LOG_UPDATE)
                        .putExtra(Constants.EXTRA_LOG_ITEM_ID, mItemId)
        );
    }

    private void finish() {
        getActivity().finish();
    }

}
