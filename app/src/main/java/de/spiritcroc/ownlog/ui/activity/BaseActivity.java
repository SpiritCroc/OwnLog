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

package de.spiritcroc.ownlog.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.Settings;
import de.spiritcroc.ownlog.data.DbHelper;

public abstract class BaseActivity extends AppCompatActivity {

    private int mThemeRes;
    private boolean mEastereggEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeRes = getThemeRes();
        mEastereggEnabled = Settings.getBoolean(this, Settings.EASTEREGG);
        setTheme(mThemeRes);
        super.onCreate(savedInstanceState);

        DbHelper.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mThemeRes != getThemeRes() ||
                mEastereggEnabled != Settings.getBoolean(this, Settings.EASTEREGG)) {
            recreate();
        }
    }

    protected int getThemeRes() {
        return Settings.getThemeNoActionBarRes(Settings.getInt(this, Settings.THEME));
    }

    protected boolean isEastereggEnabled() {
        return mEastereggEnabled;
    }

    @Override
    public void setTitle(CharSequence title) {
        if (mEastereggEnabled && getString(R.string.app_name).equals(title)) {
            super.setTitle(R.string.easteregg);
        } else {
            super.setTitle(title);
        }
    }
}
