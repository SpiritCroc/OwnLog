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
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

public class EndListeningScrollView extends ScrollView {

    private static final String TAG = EndListeningScrollView.class.getSimpleName();

    private static final boolean DEBUG = false;

    private boolean mCurrentlyEnd = true;
    private EndListener mEndListener;

    private int mBottomTriggerHeight = 100;

    public EndListeningScrollView(Context context) {
        super(context);
    }

    public EndListeningScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EndListeningScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EndListeningScrollView(Context context, AttributeSet attrs, int defStyleAttr,
                                  int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setEndListener(EndListener listener) {
        mEndListener = listener;
    }

    public boolean isAtEnd() {
        return mCurrentlyEnd;
    }

    @Override
    public void onViewAdded(View child) {
        checkBottom(child);
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        checkBottom(getChildAt(0));
    }

    private void checkBottom(View child) {
        //if (DEBUG) Log.d(TAG, "Scroll: " + child.getBottom() + " " + getHeight() + " " +
        //        getScrollY());
        int scrollY = getScrollY();
        if (scrollY == 0 ||
                child.getBottom() - getHeight() - scrollY <= mBottomTriggerHeight) {
            if (!mCurrentlyEnd) {
                mCurrentlyEnd = true;
                if (DEBUG) Log.d(TAG, "scroll: Reached bottom");
                if (mEndListener != null) {
                    mEndListener.onBottomReached();
                }
            }
        } else {
            if (mCurrentlyEnd) {
                mCurrentlyEnd = false;
                if (DEBUG) Log.d(TAG, "scroll: Left bottom");
                if (mEndListener != null) {
                    mEndListener.onBottomLeft();
                }
            }
        }
    }

    public interface EndListener {
        void onBottomReached();
        void onBottomLeft();
    }

    public void setBottomTriggerHeight(int height) {
        mBottomTriggerHeight = height;
    }

    public int getBottomTriggerHeight() {
        return mBottomTriggerHeight;
    }
}
