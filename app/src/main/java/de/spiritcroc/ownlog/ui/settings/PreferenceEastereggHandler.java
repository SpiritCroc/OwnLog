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

package de.spiritcroc.ownlog.ui.settings;

import android.preference.Preference;

class PreferenceEastereggHandler implements Preference.OnPreferenceClickListener {

    public static final int REQUIRED_HITS_NORMAL = 7;

    public static final int REQUIRED_HITS_MORE = 13;

    private int mRequiredHits;

    private static final int ALLOWED_BREAK_MILLISECONDS = 1000;

    private int mHits = 0;
    private long mLastHit = -1L;

    private Runnable mEggRunnable;

    PreferenceEastereggHandler(Preference preference, Runnable eggRunnable) {
        this(preference, eggRunnable, REQUIRED_HITS_NORMAL);
    }

    PreferenceEastereggHandler(Preference preference, Runnable eggRunnable, int requiredHits) {
        mEggRunnable = eggRunnable;
        mRequiredHits = requiredHits;

        preference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        long now = System.currentTimeMillis();
        if (now - mLastHit > ALLOWED_BREAK_MILLISECONDS) {
            mHits = 0;
        }
        mLastHit = now;
        mHits++;
        if (mHits == mRequiredHits) {
            mHits = 0;
            mEggRunnable.run();
        }
        return true;
    }
}
