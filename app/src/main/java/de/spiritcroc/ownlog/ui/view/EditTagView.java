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

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.data.TagItem;

class EditTagView extends LinearLayout {

    private TagItem mItem;
    private OnEditTagRequestListener mListener;

    public EditTagView(Context context, TagItem tagItem, OnEditTagRequestListener listener) {
        super(context);
        mItem = tagItem;
        mListener = listener;

        setGravity(Gravity.CENTER_VERTICAL);
        setOrientation(HORIZONTAL);
        setBackground(getResources().getDrawable(R.drawable.edit_tag_view_bg, context.getTheme()));

        TextView tv = new TextView(context);
        addView(tv);
        tv.setText(mItem.name);

        ImageView removeButton = new ImageView(context);
        removeButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_remove_small,
                context.getTheme()));
        int removeButtonPadding =
                getResources().getDimensionPixelSize(R.dimen.edit_log_tag_remove_button_padding);
        removeButton.setPadding(removeButtonPadding, removeButtonPadding, removeButtonPadding,
                removeButtonPadding);
        addView(removeButton);

        removeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onRemoveRequest(EditTagView.this);
            }
        });

        tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(mItem.description)) {
                    Toast.makeText(getContext(), mItem.description, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Long click listener for edit request
        OnLongClickListener longClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.onEditRequest(EditTagView.this);
                return true;
            }
        };

        tv.setOnLongClickListener(longClickListener);
        removeButton.setOnLongClickListener(longClickListener);
    }

    public TagItem getTagItem() {
        return mItem;
    }

    interface OnEditTagRequestListener {
        void onRemoveRequest(EditTagView view);
        void onEditRequest(EditTagView view);
    }
}
