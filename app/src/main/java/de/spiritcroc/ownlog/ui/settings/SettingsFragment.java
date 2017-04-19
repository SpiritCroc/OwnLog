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

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;

import de.spiritcroc.ownlog.PasswdHelper;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.Settings;
import de.spiritcroc.ownlog.data.LoadLogFiltersTask;
import de.spiritcroc.ownlog.data.LogFilter;
import de.spiritcroc.ownlog.ui.fragment.DismissDialogFragment;
import de.spiritcroc.ownlog.ui.fragment.SetPasswordDialog;

public class SettingsFragment extends BaseSettingsFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        DismissDialogFragment.OnDismissListener, PasswdHelper.RequestDbListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static final String PREF_KEY_EASTEREGG_OFF = "easteregg_off";

    private static final int DB_REQUEST_LOG_FILTERS = 1;

    private Preference mPassword;
    private ListPreference mDefaultFilter;
    private ListPreference mTheme;
    private Preference mEastereggOff;
    private CheckBoxPreference mEasteregg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mPassword = findPreference(Settings.PASSWORD);
        mDefaultFilter = (ListPreference) findPreference(Settings.DEFAULT_FILTER);
        mTheme = (ListPreference) findPreference(Settings.THEME);
        mEastereggOff = findPreference(PREF_KEY_EASTEREGG_OFF);
        mEasteregg = (CheckBoxPreference) findPreference(Settings.EASTEREGG);

        new PreferenceEastereggHandler(mEastereggOff, mEnableEastereggRunnable);
    }

    private void init() {
        updatePassword();
        setValueToSummary(mTheme);
        loadDefaultFilterPref();

        if (Settings.getBoolean(getActivity(), Settings.EASTEREGG)) {
            getPreferenceScreen().removePreference(mEastereggOff);
        } else {
            getPreferenceScreen().removePreference(mEasteregg);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Settings.THEME.equals(key)) {
            getActivity().recreate();
        } else if (Settings.DEFAULT_FILTER.equals(key)) {
            setValueToSummary(mDefaultFilter);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        if (preference == mPassword) {
            new SetPasswordDialog()
                    .setOnDismissListener(this)
                    .show(getFragmentManager(), "SetPasswordDialog");
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    @Override
    public void onDialogDismiss(DialogFragment which) {
        init();
    }

    @Override
    public void receiveWritableDatabase(SQLiteDatabase db, int requestId) {
        switch (requestId) {
            case DB_REQUEST_LOG_FILTERS:
                loadDefaultFilterPref(db);
                break;
            default:
                Log.e(TAG, "receiveWritableDatabase: unknwon requestId " + requestId);

        }
    }


    private void updatePassword() {
        mPassword.setSummary(PasswdHelper.isPasswordUnset()
                ? R.string.pref_password_summary_set
                : R.string.pref_password_summary_change);
    }

    private void loadDefaultFilterPref() {
        PasswdHelper.getWritableDatabase(getActivity(), this, DB_REQUEST_LOG_FILTERS);
    }

    private void loadDefaultFilterPref(SQLiteDatabase db) {
        new LoadDefaultFilterPrefTask(db).execute();
    }

    private class LoadDefaultFilterPrefTask extends LoadLogFiltersTask {
        LoadDefaultFilterPrefTask(SQLiteDatabase db) {
            super(db, getActivity());
        }

        @Override
        protected void onPostExecute(ArrayList<LogFilter> filters) {
            super.onPostExecute(filters);
            String[] entries = new String[filters.size()];
            String[] values = new String[filters.size()];
            for (int i = 0; i < filters.size(); i++) {
                LogFilter filter = filters.get(i);
                entries[i] = filter.name;
                values[i] = Long.toString(filter.id);
            }
            mDefaultFilter.setEntries(entries);
            mDefaultFilter.setEntryValues(values);

            setValueToSummary(mDefaultFilter);
        }
    }

    private Runnable mEnableEastereggRunnable = new Runnable() {
        @Override
        public void run() {
            Settings.put(getActivity(), Settings.EASTEREGG, true);
            getPreferenceScreen().removePreference(mEastereggOff);
            getPreferenceScreen().addPreference(mEasteregg);
            mEasteregg.setChecked(true);
        }
    };
}
