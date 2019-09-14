/*
 * Copyright (C) 2017-2019 SpiritCroc
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
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.request.target.Target;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.nio.charset.Charset;
import java.security.MessageDigest;
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
                            openFile(mAttachments.get(position));
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
                            shareFile(mAttachments.get(position));
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

    private void shareFile(LogItem.Attachment attachment) {
        Uri uri = FileHelper.getAttachmentFileShare(getActivity(), attachment);
        ShareCompat.IntentBuilder.from(getActivity())
                .addStream(uri)
                .setType(attachment.type)
                .startChooser();
    }

    private void openFile(LogItem.Attachment attachment) {
        Uri uri = FileHelper.getAttachmentFileShare(getActivity(), attachment);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, attachment.type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    protected boolean isAttachmentClickEnabled() {
        return true;
    }

    protected AttachmentViewHolder onCreateAttachmentViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.log_attachment, parent, false);
        return new AttachmentViewHolder(v);
    }

    private class AttachmentPreviewTransformation extends BitmapTransformation {
        private final String ID = AttachmentPreviewTransformation.class.getCanonicalName();
        private final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));

        @Override
        public Bitmap transform(@NonNull  BitmapPool pool, @NonNull Bitmap toTransform,
                                int outWidth, int outHeight) {
            int inSampleSize = 1;

            // We don't need fullscreen, just a small preview
            final int reqWidth = mViewWidth/mPicturePreviewScale;
            final int reqHeight = reqWidth;

            if (outHeight > reqHeight || outWidth > reqWidth) {
                final int halfHeight = outHeight/2;
                final int halfWidth = outWidth/2;
                while ((halfHeight/inSampleSize) > reqHeight/mPicturePreviewScale &&
                        (halfWidth/inSampleSize) > reqWidth/mPicturePreviewScale) {
                    inSampleSize++;
                }
            }

            return Bitmap.createScaledBitmap(toTransform, outWidth/inSampleSize,
                    outHeight/inSampleSize, /*filter=*/ true);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof AttachmentPreviewTransformation;
        }

        @Override
        public int hashCode() {
            return ID.hashCode();
        }

        @Override
        public void updateDiskCacheKey(@NonNull  MessageDigest messageDigest) {
            messageDigest.update(ID_BYTES);
        }
    }

    protected void onBindAttachmentViewHolder(AttachmentViewHolder holder, int position) {
        LogItem.Attachment attachment = mAttachments.get(position);
        File attachmentFile = FileHelper.getAttachmentFile(getActivity(), attachment);
        holder.mNameTextView.setText(attachment.name);
        holder.mSizeTextView.setText(Util.formatFileSize(getActivity(), attachmentFile.length()));
        // Preview
        holder.mPreviewImageView.setVisibility(View.VISIBLE);
        Glide.with(holder.mPreviewImageView)
                .load(attachmentFile.getAbsolutePath())
                .transform(new FitCenter(), new AttachmentPreviewTransformation())
                .override(Target.SIZE_ORIGINAL)
                .into(holder.mPreviewImageView);
    }

    public interface OnUpdateListener {
        void onAttachmentsUpdate();
    }
}
