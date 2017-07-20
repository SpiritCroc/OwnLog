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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.FileHelper;
import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.Util;
import de.spiritcroc.ownlog.data.LoadLogItemAttachmentsTask;
import de.spiritcroc.ownlog.data.LogItem;

public class LogAttachmentsShowFragment extends Fragment
        implements PasswdHelper.RequestDbListener {

    private static final String TAG = LogAttachmentsShowFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final int DB_REQUEST_LOAD_CONTENT = 1;

    private long mLogId = -1;

    private boolean mLoadingContent = false;
    private boolean mReloadRequired = true;
    private ArrayList<LogItem.Attachment> mAttachments = new ArrayList<>();
    private AttachmentsAdapter mAttachmentsAdapter;

    private View mTitleView;
    private RecyclerView mAttachmentsContainer;
    private View.OnClickListener mTitleViewClickListener;
    private OnUpdateListener mOnUpdateListener;

    private int mViewWidth = -1;
    private int mPicturePreviewScale = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mLogId = savedInstanceState.getLong(Constants.EXTRA_LOG_ITEM_ID, -1);
            if (mLogId == -1) {
                Log.w(TAG, "onCreate: could not find out log id from saved instance");
            }
        } else if (getArguments() == null) {
            Log.e(TAG, "onCreate: getArguments() == null");
        } else {
            mLogId = getArguments().getLong(Constants.EXTRA_LOG_ITEM_ID, -1);
            if (mLogId == -1) {
                Log.w(TAG, "onCreate: could not find out log id");
            }
        }

        calculateViewWidth();
        mPicturePreviewScale =
                getResources().getInteger(R.integer.attachment_picture_preview_scale);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(getLayoutResource(), container, false);
        mTitleView = v.findViewById(R.id.attachments_title);
        if (mTitleViewClickListener != null) {
            mTitleView.setOnClickListener(mTitleViewClickListener);
        }
        mAttachmentsContainer = (RecyclerView) v.findViewById(R.id.attachments_container);
        mAttachmentsAdapter = new AttachmentsAdapter();
        mAttachmentsContainer.setAdapter(mAttachmentsAdapter);
        return v;
    }

    private void calculateViewWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mViewWidth = displayMetrics.widthPixels
                - getResources().getDimensionPixelSize(R.dimen.attachment_margin)
                - getResources().getDimensionPixelSize(R.dimen.attachment_padding);
    }

    protected int getLayoutResource() {
        return R.layout.log_attachments_show;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mReloadRequired) {
            loadContent();
            mReloadRequired = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(Constants.EXTRA_LOG_ITEM_ID, mLogId);
    }

    public void setTitleOnClickListener(View.OnClickListener listener) {
        mTitleViewClickListener = listener;
        if (mTitleView != null) {
            mTitleView.setOnClickListener(mTitleViewClickListener);
        }
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        mOnUpdateListener = listener;
    }

    public long getLogId() {
        return mLogId;
    }

    public void setLogId(long id) {
        mLogId = id;
        loadContent();
    }

    public void setReloadRequired() {
        mReloadRequired = true;
    }

    public boolean hasAttachments() {
        return !mAttachments.isEmpty();
    }

    protected LogItem.Attachment getAttachment(int position) {
        return mAttachments.get(position);
    }

    public void loadContent() {
        if (mLogId == -1) {
            Log.d(TAG, "loadContent: skip because of unset log id");
            // Empty list
            loadContent(new ArrayList<LogItem.Attachment>());
            return;
        }
        if (DEBUG) Log.d(TAG, "loadContent: start new: " + !mLoadingContent);
        if (!mLoadingContent) {
            mLoadingContent = true;
            PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_LOAD_CONTENT);
        }
    }

    private void loadContent(ArrayList<LogItem.Attachment> attachments) {
        mAttachments = attachments;
        mAttachmentsAdapter.notifyDataSetChanged();
        if (mOnUpdateListener != null) {
            mOnUpdateListener.onAttachmentsUpdate();
        }
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        switch (requestId) {
            case DB_REQUEST_LOAD_CONTENT:
                new LoadContentTask(db).execute(mLogId);
                break;
            default:
                Log.e(TAG, "receiveWritableDatabase: unknwon requestId " + requestId);
        }
    }

    private class LoadContentTask extends LoadLogItemAttachmentsTask {

        public LoadContentTask(SQLiteDatabase db) {
            super(db);
        }

        @Override
        public void onPostExecute(ArrayList<LogItem.Attachment> result) {
            super.onPostExecute(result);

            mLoadingContent = false;

            if (DEBUG) {
                Log.d(TAG, "Attachments found: " + result.size());
            }
            for (LogItem.Attachment a: result) {
                Log.d(TAG, "" + a);
            }
            loadContent(result);
        }
    }

    protected class AttachmentViewHolder extends RecyclerView.ViewHolder {
        public TextView mNameTextView;
        public TextView mSizeTextView;
        public ImageView mPreviewImageView;

        public AttachmentViewHolder(View view) {
            super(view);
            mNameTextView = (TextView) view.findViewById(R.id.attachment_name);
            mSizeTextView = (TextView) view.findViewById(R.id.attachment_size);
            mPreviewImageView = (ImageView) view.findViewById(R.id.attachment_preview);
            if (isAttachmentClickEnabled()) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        if (position < 0 || position >= mAttachments.size()) {
                            Log.e(TAG, "Attachment click at invalid position " + position
                                    + ", attachments count is " + mAttachments.size());
                        } else {
                            new MyOpenFileTask().execute(mAttachments.get(position));
                        }
                    }
                });
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int position = getAdapterPosition();
                        if (position < 0 || position >= mAttachments.size()) {
                            Log.e(TAG, "Attachment long click at invalid position " + position
                                    + ", attachments count is " + mAttachments.size());
                        } else {
                            new MyShareFileTask().execute(mAttachments.get(position));
                        }
                        return true;
                    }
                });
            }
        }
    }

    private class AttachmentsAdapter extends RecyclerView.Adapter<AttachmentViewHolder> {

        @Override
        public AttachmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return onCreateAttachmentViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(AttachmentViewHolder holder, int position) {
           onBindAttachmentViewHolder(holder, position);
        }

        @Override
        public int getItemCount() {
            return mAttachments.size();
        }

    }

    private abstract class MyAbstractShareFileTask extends FileHelper.ShareFileTask {
        protected String mFileType;
        @Override
        protected Context getContext() {
            return getActivity();
        }
        @Override
        protected Uri doInBackground(LogItem.Attachment... attachments) {
            if (attachments.length == 1) {
                mFileType = attachments[0].type;
            } // else: super will throw exception
            return super.doInBackground(attachments);
        }
        @Override
        protected void onPostExecute(Uri result) {
            if (getActivity() == null) {
                Log.w(TAG, "Discarding share file result: activity is null");
                return;
            }
            if (DEBUG) Log.d(TAG, "ShareFileTask: result is " + result);
            if (result == null) {
                Toast.makeText(getActivity(), R.string.error_io_out, Toast.LENGTH_LONG).show();
            } else {
                launchIntent(result);
            }
        }
        protected abstract void launchIntent(Uri uri);
    }

    private class MyShareFileTask extends MyAbstractShareFileTask {
        @Override
        protected void launchIntent(Uri uri) {
            ShareCompat.IntentBuilder.from(getActivity())
                    .addStream(uri)
                    .setType(mFileType)
                    .startChooser();
        }
    }

    private class MyOpenFileTask extends MyAbstractShareFileTask {
        @Override
        protected void launchIntent(Uri uri) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mFileType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    protected boolean isAttachmentClickEnabled() {
        return true;
    }

    protected AttachmentViewHolder onCreateAttachmentViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.log_attachment, parent, false);
        return new AttachmentViewHolder(v);
    }

    protected void onBindAttachmentViewHolder(AttachmentViewHolder holder, int position) {
        LogItem.Attachment attachment = mAttachments.get(position);
        holder.mNameTextView.setText(attachment.name);
        holder.mSizeTextView.setText(Util.formatFileSize(getActivity(), attachment.data.length));
        // Preview
        try {
            // TODO speed up using cache/async loading?
            // https://developer.android.com/topic/performance/graphics/load-bitmap.html
            // Check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(attachment.data, 0,
                    attachment.data.length, options);
            if (options.outWidth != -1 && options.outHeight != -1) {
                // Calculate inSampleSize
                calculateInSampleSize(options);

                // Load image
                options.inJustDecodeBounds = false;
                holder.mPreviewImageView.setImageBitmap(
                        BitmapFactory.decodeByteArray(attachment.data, 0, attachment.data.length,
                                options));
                holder.mPreviewImageView.setVisibility(View.VISIBLE);
            } else {
                // Not an image
                holder.mPreviewImageView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            holder.mPreviewImageView.setVisibility(View.GONE);
        }
    }

    private void calculateInSampleSize(BitmapFactory.Options options) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;

        // We don't need fullscreen, just a small preview
        final int reqWidth = mViewWidth/mPicturePreviewScale;
        final int reqHeight = reqWidth;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height/2;
            final int halfWidth = width/2;
            while ((halfHeight/inSampleSize) > reqHeight/mPicturePreviewScale &&
                    (halfWidth/inSampleSize) > reqWidth/mPicturePreviewScale) {
                inSampleSize++;
            }
        }
        options.inSampleSize = inSampleSize;
    }

    public interface OnUpdateListener {
        void onAttachmentsUpdate();
    }
}
