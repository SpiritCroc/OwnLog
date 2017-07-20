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

package de.spiritcroc.ownlog.ui.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;

import java.util.HashMap;
import java.util.List;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.data.TagItem;
import de.spiritcroc.ownlog.ui.fragment.TagItemEditFragment;

public class EditTagsView extends FlexboxLayout implements View.OnClickListener,
        View.OnLongClickListener, EditTagView.OnEditTagRequestListener {

    private static final String TAG = EditTagsView.class.getSimpleName();

    private View mAddTagButton;
    private EditTagsProvider mTagsProvider;
    private int mInsertTagPosition;
    private int mAddTagButtonId;
    private int mEditTagMessageId;

    private AvailableTagsFilter mAvailableTagsFilter = new AvailableTagsFilter() {
        @Override
        public boolean shouldShowTag(TagItem item) {
            return !mTagsProvider.getSetTags().contains(item);
        }
    };

    private BroadcastReceiver mTagBroadcastReceiver;

    public EditTagsView(Context context) {
        this(context, null);
    }

    public EditTagsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditTagsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray ta = getContext().obtainStyledAttributes(attrs,
                    R.styleable.EditTagsView);
            mAddTagButtonId = ta.getResourceId(R.styleable.EditTagsView_addTagButton, 0);
            mEditTagMessageId = ta.getResourceId(R.styleable.EditTagsView_editTagMessageView, 0);
            ta.recycle();
        }
    }

    public void setTagsProvider(EditTagsProvider provider) {
        mTagsProvider = provider;
    }

    public void setAvailableTagsFilter(AvailableTagsFilter filter) {
        mAvailableTagsFilter = filter;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        LocalBroadcastManager.getInstance(getContext())
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
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mTagBroadcastReceiver);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mAddTagButton = findViewById(mAddTagButtonId);
        View editTagMsg = findViewById(mEditTagMessageId);
        if (mAddTagButton != null) {
            mAddTagButton.setOnClickListener(this);
            mAddTagButton.setOnLongClickListener(this);
        }
        if (editTagMsg == null) {
            mInsertTagPosition = 0;
        } else {
            mInsertTagPosition = indexOfChild(editTagMsg) + 1;
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mAddTagButton) {
            showAddTagMenu();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == mAddTagButton) {
            Toast.makeText(getContext(), R.string.edit_log_tags_add, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public void onEditTagResult(TagItem item) {
        // Update tag by replacing
        List<TagItem> availableTags = mTagsProvider.getAvailableTags();
        List<TagItem> setTags = mTagsProvider.getSetTags();
        int index = availableTags.indexOf(item);
        if (index >= 0) {
            availableTags.remove(index);
            availableTags.add(index, item);
        }
        index = setTags.indexOf(item);
        if (index >= 0) {
            setTags.remove(index);
            setTags.add(index, item);
        }
        updateContent();
    }

    public void onAddTagResult(TagItem item) {
        if (mTagsProvider == null) {
            Log.e(TAG, "onAddTagResult: no tags provider set");
            return;
        }
        // Add tag to available tags + selection
        mTagsProvider.getAvailableTags().add(item);
        mTagsProvider.getSetTags().add(item);
        mTagsProvider.onSetTagsChanged();
        updateContent();
    }

    public void onDeleteTagResult(TagItem item) {
        if (mTagsProvider == null) {
            Log.e(TAG, "onDeleteTagResult: no tags provider set");
            return;
        }
        // Tag was deleted, so remove it here as well
        mTagsProvider.getAvailableTags().remove(item);
        if (mTagsProvider.getSetTags().remove(item)) {
            mTagsProvider.onSetTagsChanged();
        }
        // Notify provider about change
        mTagsProvider.onDeleteTag(item);
        updateContent();
    }

    @Override
    public void onRemoveRequest(EditTagView view) {
        mTagsProvider.getSetTags().remove(view.getTagItem());
        removeView(view);
        mTagsProvider.onSetTagsChanged();
    }

    @Override
    public void onEditRequest(EditTagView view) {
        if (mTagsProvider == null) {
            Log.e(TAG, "onEditRequest: no tags provider set");
            return;
        }
        new TagItemEditFragment().setEditItemId(view.getTagItem().id)
                .show(mTagsProvider.getActivity().getFragmentManager(), "TagItemEditFragment");
    }

    public void updateContent() {
        // Remove any present tag views
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (v instanceof EditTagView) {
                removeView(v);
            }
        }
        // Ensure we have a tags provider
        if (mTagsProvider == null) {
            Log.e(TAG, "Cannot load tags: no tags provider");
            return;
        }
        // Add tag views
        List<TagItem> setTags = mTagsProvider.getSetTags();
        for (int i = setTags.size()-1; i >= 0; i--) {
            // Remove in reverse direction since we're not adding at the end, but at a fixed
            // position in order to get them ordered correctly
            addView(new EditTagView(getContext(), setTags.get(i), this), mInsertTagPosition,
                    generateTagViewLayoutParams());
        }
    }

    private FlexboxLayout.LayoutParams generateTagViewLayoutParams() {
        FlexboxLayout.LayoutParams lp =
                new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT);
        int margin = getContext().getResources().getDimensionPixelSize(R.dimen.edit_log_tag_margin);
        lp.setMargins(margin, margin, margin, margin);
        return lp;
    }

    private void showAddTagMenu() {
        // Ensure we have a tags provider
        if (mTagsProvider == null) {
            Log.e(TAG, "Cannot load tags: no tags provider");
            return;
        }
        // Use context from activity instead of getContext() to separate popup from
        // this view's theme (padding etc.)
        PopupMenu popupMenu = new PopupMenu(mTagsProvider.getActivity(), mAddTagButton);
        Menu menu = popupMenu.getMenu();
        final List<TagItem> availableTags = mTagsProvider.getAvailableTags();
        final List<TagItem> setTags = mTagsProvider.getSetTags();
        final HashMap<String, TagItem> tagMap = new HashMap<>();
        for (TagItem tag: availableTags) {
            if (mAvailableTagsFilter.shouldShowTag(tag)) {
                menu.add(tag.name);
                tagMap.put(tag.name, tag);
            }
        }
        menu.add(R.string.edit_log_tags_add_new);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String name = menuItem.getTitle().toString();
                if (tagMap.containsKey(name)) {
                    setTags.add(tagMap.get(name));
                    mTagsProvider.onSetTagsChanged();
                    updateContent();
                } else {
                    new TagItemEditFragment().show(mTagsProvider.getActivity().getFragmentManager(),
                            "TagItemEditFragment");
                }
                return false;
            }
        });
        popupMenu.show();
    }

    public interface EditTagsProvider {
        List<TagItem> getAvailableTags();
        List<TagItem> getSetTags();
        void onDeleteTag(TagItem item);
        void onSetTagsChanged();
        // We need activity to get fragmentManager
        Activity getActivity();
    }

    public interface AvailableTagsFilter {
        boolean shouldShowTag(TagItem tagItem);
    }
}
