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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import de.spiritcroc.ownlog.BuildConfig;
import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.DateFormatter;
import de.spiritcroc.ownlog.FileHelper;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.TagFormatter;
import de.spiritcroc.ownlog.Util;
import de.spiritcroc.ownlog.data.DbContract;
import de.spiritcroc.ownlog.data.DbHelper;
import de.spiritcroc.ownlog.data.ExternalDbReadHelper;
import de.spiritcroc.ownlog.data.ImportItemInfo;
import de.spiritcroc.ownlog.data.LoadLogItemAttachmentsTask;
import de.spiritcroc.ownlog.data.LoadLogItemsTask;
import de.spiritcroc.ownlog.data.LoadTagItemsTask;
import de.spiritcroc.ownlog.data.LogItem;
import de.spiritcroc.ownlog.data.TagItem;
import de.spiritcroc.ownlog.ui.PermissionRequester;
import de.spiritcroc.ownlog.ui.activity.SingleFragmentActivity;

public class ImportLogFragment extends BaseFragment implements PasswdHelper.RequestDbListener,
        AdapterView.OnItemClickListener, PermissionRequester {

    private static final String TAG = ImportLogFragment.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static final String EXTRA_URI = "de.spiritcroc.ownlog.ImportDialog.EXTRA_URI";

    private static final int DB_REQUEST_LOCAL = 1;
    private static final int DB_REQUEST_IMPORT = 2;

    private static final int PERMISSION_REQUEST_URI = 1;

    private Uri mFileUri;
    private FileHelper.ImportFiles mImportFiles;
    private LogItem[] mLogItems;
    private ImportInfo[] mImportInfos;
    private ArrayList<LogItem.Attachment>[] mImportAttachments;
    private ArrayList<TagItem> mLocalAvailableTags;
    private LogArrayAdapter mAdapter;
    private ImportItemInfo.Strategy mImportStrategy = ImportItemInfo.Strategy.LATEST;
    // Strategy index for selection dialog
    private int mImportStrategySelection = 0;

    private ExternalDbReadHelper mDbHelper;

    public static void show(Context context, Uri uri) {
        Intent intent = new Intent(context, SingleFragmentActivity.class)
                .putExtra(Constants.EXTRA_FRAGMENT_CLASS,
                        ImportLogFragment.class.getName())
                .putExtra(Constants.EXTRA_TITLE, context.getString(R.string.title_import_log));
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString(EXTRA_URI, uri.toString());
        intent.putExtra(Constants.EXTRA_FRAGMENT_BUNDLE, fragmentArgs);
        context.startActivity(intent);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileUri = Uri.parse(getArguments().getString(EXTRA_URI));
        if (DEBUG) Log.d(TAG, "file uri is " + mFileUri);
        if (FileHelper.checkFileUriReadPermissions(getActivity(), mFileUri,
                PERMISSION_REQUEST_URI)) {
            new ExtractDbTask().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_URI:
                // Anything denied that we need?
                for (int result: grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                R.string.error_permission_denied_read_external_storage,
                                Toast.LENGTH_LONG).show();
                        getActivity().finish();
                        return;
                    }
                }
                // Read it now
                new ExtractDbTask().execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_import_log, container, false);
        ListView listView = (ListView) v.findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_import_log, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_strategy:
                promptSelectStrategy();
                return true;
            case R.id.action_import:
                promtDoImport();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void promptSelectStrategy() {
        switch (mImportStrategy) {
            case LATEST:
                mImportStrategySelection = 0;
                break;
            case IF_NEW:
                mImportStrategySelection = 1;
                break;
            case OVERWRITE:
                mImportStrategySelection = 2;
                break;
            default:
                Log.wtf(TAG, "promptSelectStrategy: unhandled selection " + mImportStrategy);
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.import_dialog_strategy_title)
                .setSingleChoiceItems(R.array.import_strategies, mImportStrategySelection,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mImportStrategySelection = which;
                            }
                        })
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (mImportStrategySelection) {
                            case 0:
                                mImportStrategy = ImportItemInfo.Strategy.LATEST;
                                break;
                            case 1:
                                mImportStrategy = ImportItemInfo.Strategy.IF_NEW;
                                break;
                            case 2:
                                mImportStrategy = ImportItemInfo.Strategy.OVERWRITE;
                                break;
                            default:
                                Log.wtf(TAG, "promptSelectStrategy: unhandled selection " +
                                        mImportStrategySelection);
                        }
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close
                    }
                })
                .show();
    }

    private void promtDoImport() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.import_dialog_title)
                .setMessage(R.string.import_dialog_message)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PasswdHelper.getWritableDatabase(getActivity(), ImportLogFragment.this,
                                DB_REQUEST_IMPORT);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close
                    }
                })
                .show();
    }

    private boolean shouldSelect(ImportInfo importItemInfo) {
        ImportItemInfo.ChangeStatus changeStatus = importItemInfo.getChangeStatus();
        ImportItemInfo.Strategy strategy = importItemInfo.getStrategy();
        if (strategy == ImportItemInfo.Strategy.GLOBAL) {
            strategy = mImportStrategy;
        }
        return strategy == ImportItemInfo.Strategy.OVERWRITE ||
                    (strategy == ImportItemInfo.Strategy.IF_NEW &&
                            changeStatus == ImportItemInfo.ChangeStatus.NEW) ||
                    (strategy != ImportItemInfo.Strategy.KEEP &&
                            (changeStatus == ImportItemInfo.ChangeStatus.CHANGED ||
                            changeStatus == ImportItemInfo.ChangeStatus.UPDATED ||
                            changeStatus == ImportItemInfo.ChangeStatus.NEW));
    }

    private void setSelect(ImportItemInfo importItemInfo, boolean select) {
        importItemInfo.setStrategy(select
                ? ImportItemInfo.Strategy.OVERWRITE
                : ImportItemInfo.Strategy.KEEP);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Change import selection
        setSelect(mImportInfos[position], !shouldSelect(mImportInfos[position]));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        switch (requestId) {
            case DB_REQUEST_LOCAL:
                new CompareBackupTask(db).execute();
                return;
            case DB_REQUEST_IMPORT:
                new ImportBackupTask(db).execute();
                return;
            default:
                Log.e(TAG, "receiveWritableDatabase: unknown requestId " + requestId);
        }
    }

    private void onPreDoInBackground(String message) {
        setHasOptionsMenu(false);
        View view = getView();
        if (view != null) {
            view.findViewById(R.id.list_view).setVisibility(View.GONE);
            view.findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.progress_message)).setText(message);
        }
    }

    private void onPostDoInBackground() {
        setHasOptionsMenu(true);
        View view = getView();
        if (view != null) {
            view.findViewById(R.id.progress_layout).setVisibility(View.GONE);
            view.findViewById(R.id.list_view).setVisibility(View.VISIBLE);
        }
    }

    private class ExtractDbTask extends AsyncTask<Void, Void, FileHelper.ImportFiles> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onPreDoInBackground(getString(R.string.import_read_message));
        }
        @Override
        protected FileHelper.ImportFiles doInBackground(Void... nothing) {
            try {
                return FileHelper.unpackImport(getActivity(),
                        getActivity().getContentResolver().openInputStream(mFileUri));
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Import file: not found for uri " + mFileUri);
                return null;
            }
        }
        @Override
        protected void onPostExecute(FileHelper.ImportFiles result) {
            super.onPostExecute(result);
            onPostDoInBackground();
            if (result != null) {
                // Database file should exist now
                mImportFiles = result;
                loadImportDb(null);
            } else {
                // Invalid zip: notify user and close activity
                invalidImport();
            }
        }
    }

    private void invalidImport() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.error_import_failure_title)
                .setMessage(R.string.error_import_failure_message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .show();
    }

    private void loadImportDb(String passwd) {
        if (mDbHelper == null) {
             mDbHelper = new ExternalDbReadHelper(mImportFiles.logDbFile.getAbsolutePath());
        }
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getReadableDatabase(passwd);
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (DbHelper.UnsupportedUpgradeException e) {
            Log.w(TAG, "Import: unsupported upgrade from " + e.oldVersion);
            // Unsupported db version: notify user and close activity
            invalidImport();
            // No more prompting for password
            return;
        }
        if (db != null) {
            new LoadBackupTask(db).execute();
        } else {
            // Prompt for password
            final Activity activity = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final View view =
                    activity.getLayoutInflater().inflate(R.layout.dialog_request_password, null);

            final EditText editPassword = (EditText) view.findViewById(R.id.edit_password);

            final AlertDialog alertDialog = builder
                    .setTitle(R.string.import_dialog_request_password)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadImportDb(editPassword.getText().toString());
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.finish();
                                    mDbHelper.close();
                                }
                    })
                    .show();

            // Edit text requires user interaction, so show keyboard
            alertDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private ImportInfo[] createInitialImportInfos(SQLiteDatabase localDb) {
        mLocalAvailableTags = LoadTagItemsTask.loadAvailableTags(localDb, null);
        ImportInfo[] result = new ImportInfo[mLogItems.length];
        for (int index = 0; index < result.length; index++) {
            String selection = DbContract.Log._ID + " = " + mLogItems[index].id;
            ArrayList<LogItem> localLogItem = LoadLogItemsTask.loadLogItems(localDb, selection,
                    LoadLogItemsTask.getFullProjection(), null, true, true);
            if (localLogItem.isEmpty()) {
                result[index] = new ImportInfo(ImportItemInfo.ChangeStatus.NEW);
            } else {
                if (localLogItem.size() != 1) {
                    Log.wtf(TAG, "Multiple log entries with id " + mLogItems[index].id);
                }
                LogItem localItem = localLogItem.get(0);
                LogItem importItem = mLogItems[index];
                if (localItem.differs(importItem)) {
                    if (localItem.newerThan(importItem)) {
                        result[index] = new ImportInfo(ImportItemInfo.ChangeStatus.DEPRECATED);
                    } else if (importItem.newerThan(localItem)) {
                        result[index] = new ImportInfo(ImportItemInfo.ChangeStatus.UPDATED);
                    } else {
                        result[index] = new ImportInfo(ImportItemInfo.ChangeStatus.CHANGED);
                    }
                } else {
                    result[index] = new ImportInfo(ImportItemInfo.ChangeStatus.SAME);
                }

                // Check tag changes
                result[index].tagChangeStatus = Util.getListDiffChangeStatus(localItem.tags,
                        importItem.tags);

                // Check attachment changes
                ArrayList<LogItem.Attachment> localAttachments =
                        LoadLogItemAttachmentsTask.loadAttachments(localDb,
                                LoadLogItemAttachmentsTask.getSelectionFromLogIds(
                                        mLogItems[index].id),
                                LoadLogItemAttachmentsTask.getFullProjection(),
                                null);
                result[index].attachmentChangeStatus =
                        Util.getListDiffChangeStatus(localAttachments, mImportAttachments[index]);
            }
        }
        return result;
    }

    private class LoadBackupTask extends LoadLogItemsTask {
        private LoadBackupTask(SQLiteDatabase importDb) {
            super(importDb);
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onPreDoInBackground(getString(R.string.import_read_message));
        }
        @Override
        protected ArrayList<LogItem> doInBackground(Void... params) {
            ArrayList<LogItem> result = super.doInBackground(params);
            mImportAttachments = new ArrayList[result.size()];
            for (int i = 0; i < result.size(); i++) {
                mImportAttachments[i] = LoadLogItemAttachmentsTask.loadAttachments(mDb,
                        LoadLogItemAttachmentsTask.getSelectionFromLogIds(result.get(i).id),
                        LoadLogItemAttachmentsTask.getFullProjection(),
                        null);
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<LogItem> result) {
            super.onPostExecute(result);
            onPostDoInBackground();
            mDb.close();
            mLogItems = result.toArray(new LogItem[result.size()]);
            if (getActivity() == null) {
                Log.w(TAG, "Content loaded, but activity is null");
                return;
            }
            PasswdHelper.getWritableDatabase(getActivity(), ImportLogFragment.this,
                    DB_REQUEST_LOCAL);
        }

        @Override
        protected boolean shouldCheckAttachments() {
            return true;
        }
        @Override
        protected boolean doInBackgroundClosesDb() {
            return false;
        }
    }

    private class CompareBackupTask extends AsyncTask<Void, Void, ImportInfo[]> {
        private SQLiteDatabase mLocalDb;
        CompareBackupTask(SQLiteDatabase localDb) {
            mLocalDb = localDb;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onPreDoInBackground(getString(R.string.import_compare_message));
        }
        @Override
        protected ImportInfo[] doInBackground(Void... params) {
            return createInitialImportInfos(mLocalDb);
        }

        @Override
        protected void onPostExecute(ImportInfo[] result) {
            super.onPostExecute(result);
            onPostDoInBackground();
            mLocalDb.close();
            mImportInfos = result;
            if (getActivity() == null) {
                Log.w(TAG, "Content loaded, but activity is null");
                return;
            }
            mAdapter = new LogArrayAdapter(getActivity(), R.layout.log_list_item, mLogItems);
            if (getView() == null) {
                Log.w(TAG, "Content loaded, but view is null");
                return;
            }
            final ListView listView = (ListView) getView().findViewById(R.id.list_view);
            listView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void importLogItem(SQLiteDatabase db, LogItem logItem, ImportInfo importInfo,
                               ArrayList<LogItem.Attachment> attachments) {
        if (!shouldSelect(importInfo)) {
            // Not selected -> not imported
            return;
        }
        boolean add = importInfo.tagChangeStatus == ImportItemInfo.ChangeStatus.NEW;
        if (!add) {
            // Remove previous entry (including tags and attachments)
            DbHelper.removeLogItemsFromDb(getActivity(), db, logItem);
        }
        // Add entry
        DbHelper.saveLogItemToDb(logItem, db, true, true);
        // Ensure tag existence
        for (TagItem tag: logItem.tags) {
            if (!mLocalAvailableTags.contains(tag)) {
                DbHelper.saveTagItem(db, tag, true);
                mLocalAvailableTags.add(tag);
            }
        }
        // Add tags
        DbHelper.saveLogTagsToDb(logItem, db, logItem.tags, new ArrayList<TagItem>());
        // Add attachments
        for (LogItem.Attachment attachment: attachments) {
            if (FileHelper.importAttachmentFile(getActivity(), attachment, mImportFiles)) {
                DbHelper.addLogAttachmentDbEntry(db, attachment);
            } else {
                // TODO broken backup notification
                Log.e(TAG, "Failed to import attachment file " + attachment);
            }
        }
    }

    private class ImportBackupTask extends AsyncTask<Void, Integer, Exception> {
        private SQLiteDatabase mLocalDb;
        ImportBackupTask(SQLiteDatabase localDb) {
            mLocalDb = localDb;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // TODO non-indeterminate progress?
            onPreDoInBackground(getString(R.string.import_import_message));
        }
        @Override
        protected Exception doInBackground(Void... nothing) {
            for (int i = 0; i < mLogItems.length; i++) {
                importLogItem(mLocalDb, mLogItems[i], mImportInfos[i], mImportAttachments[i]);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            // Broadcast log update
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                    new Intent(Constants.EVENT_LOG_UPDATE));
            // User feedback + close
            if (result == null) {
                Toast.makeText(getActivity().getApplicationContext(), R.string.import_successful,
                        Toast.LENGTH_LONG).show();
                getActivity().finish();
            } else {
                Log.e(TAG, "Importing backup failed", result);
                Toast.makeText(getActivity().getApplicationContext(), R.string.import_failure,
                        Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
        }
    }

    private class LogArrayAdapter extends ArrayAdapter<LogItem> {

        LogItem[] mLogItems;

        public LogArrayAdapter(Context context, int resource, LogItem[] objects) {
            super(context, resource, objects);
            mLogItems = objects;
        }

        @Override
        public @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LogItemHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.import_log_list_item, parent, false);

                holder = new LogItemHolder();
                holder.date = (TextView) convertView.findViewById(R.id.date);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.tag = (TextView) convertView.findViewById(R.id.tag);
                holder.status = (TextView) convertView.findViewById(R.id.status);
                holder.attachment = convertView.findViewById(R.id.attachment);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);

                convertView.setTag(holder);
            } else {
                holder = (LogItemHolder) convertView.getTag();
            }

            LogItem item = getItem(position);

            holder.date.setText(DateFormatter.getAutoDateFormatted(item.time));
            String statusText;
            switch (mImportInfos[position].getChangeStatus()) {
                case NEW:
                    statusText = getString(R.string.import_change_status_new);
                    break;
                case SAME:
                    statusText = getString(R.string.import_change_status_same);
                    break;
                case CHANGED:
                    statusText = getString(R.string.import_change_status_changed);
                    break;
                case UPDATED:
                    statusText = getString(R.string.import_change_status_updated);
                    break;
                case DEPRECATED:
                    statusText = getString(R.string.import_change_status_deprecated);
                    break;
                default:
                    Log.e(TAG, "No change status text for " +
                            mImportInfos[position].getChangeStatus());
                    statusText = "";
            }
            if (mImportInfos[position].getChangeStatus() != ImportItemInfo.ChangeStatus.NEW) {
                switch (mImportInfos[position].attachmentChangeStatus) {
                    case UPDATED:
                        statusText += "\n" +
                                getString(R.string.import_change_attachment_status_updated);
                        break;
                    case DEPRECATED:
                        statusText += "\n" +
                                getString(R.string.import_change_attachment_status_deprecated);
                        break;
                    case CHANGED:
                        statusText += "\n" +
                                getString(R.string.import_change_attachment_status_changed);
                        break;
                }
                switch (mImportInfos[position].tagChangeStatus) {
                    case UPDATED:
                        statusText += "\n" +
                                getString(R.string.import_change_tags_status_updated);
                        break;
                    case DEPRECATED:
                        statusText += "\n" +
                                getString(R.string.import_change_tags_status_deprecated);
                        break;
                    case CHANGED:
                        statusText += "\n" +
                                getString(R.string.import_change_tags_status_changed);
                        break;
                }
            }
            holder.status.setText(statusText);
            holder.title.setText(TextUtils.isEmpty(item.title) ? item.content : item.title);
            holder.tag.setText(TagFormatter.formatTags(getResources(), item.tags));
            holder.attachment.setVisibility(item.hasAttachments ? View.VISIBLE : View.GONE);
            holder.checkBox.setChecked(shouldSelect(mImportInfos[position]));

            return convertView;
        }
    }

    private static class LogItemHolder {
        TextView date;
        TextView title;
        TextView tag;
        TextView status;
        View attachment;
        CheckBox checkBox;
    }

    private static class ImportInfo extends ImportItemInfo {
        ChangeStatus tagChangeStatus;
        ChangeStatus attachmentChangeStatus;
        ImportInfo(ChangeStatus changeStatus) {
            super(changeStatus);
        }
    }
}
