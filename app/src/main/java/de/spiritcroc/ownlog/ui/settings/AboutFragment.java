/*
 * Copyright (C) 2017-2018 SpiritCroc
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

import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import de.spiritcroc.ownlog.BuildConfig;
import de.spiritcroc.ownlog.R;

public class AboutFragment extends BaseSettingsFragment {

    private static final String ABOUT_APP = "about_app";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);

        Preference appPref = findPreference(ABOUT_APP);
        appPref.setSummary(getString(R.string.about_app_summary,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        new PreferenceEastereggHandler(appPref, mEastereggRunnable);
    }

    private Runnable mEastereggRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getActivity(), R.string.easteregg_1, Toast.LENGTH_SHORT).show();
        }
    };
}
