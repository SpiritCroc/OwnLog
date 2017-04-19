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

package de.spiritcroc.ownlog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public abstract class Settings {

    private static final String TAG = Settings.class.getSimpleName();

    /**
     * Key for the preference leading to a password change.
     * Password not actually saved in settings, but used for database access.
     */
    public static final String PASSWORD = "pref_password";

    /**
     * The theme to use within the app.
     * Type: integer (internally saved as string for ListView usage)
     *
     * Currently available values:
     * 0 - Light
     * 1 - Dark
     */
    public static final String THEME = "pref_theme";

    /**
     * Selected filter on app start
     */
    public static final String DEFAULT_FILTER = "default_filter";

    /**
     * SimpleDateFormat string for the log list section headers
     */
    public static final String DATE_FORMAT_OVERVIEW_1 = "pref_date_format_overview_1";

    /**
     * SimpleDateFormat string for the log entry date (highlighted part)
     */
    public static final String DATE_FORMAT_OVERVIEW_2 = "pref_date_format_overview_2";

    /**
     * SimpleDateFormat string for the log entry date (detailed part)
     */
    public static final String DATE_FORMAT_OVERVIEW_3 = "pref_date_format_overview_3";

    /**
     * SimpleDateFormat string for detailed titles
     */
    public static final String DATE_FORMAT_FULL_DATE_TIME = "pref_date_format_full_date_time";

    /**
     * SimpleDateFormat string for combined titles
     */
    public static final String DATE_FORMAT_DATE_FOR_TITLE = "pref_date_format_date_for_title";

    /**
     * Every app needs its easteregg(s).
     * True if enabled, false otherwise
     */
    public static final String EASTEREGG = "easteregg";

    public static class ThemeNoActionBar {
        public static final int LIGHT = R.style.AppThemeLight_NoActionBar;
        public static final int DARK = R.style.AppThemeDark_NoActionBar;
    }

    public static final int[] themesNoActionBar = new int[] {
            ThemeNoActionBar.LIGHT,
            ThemeNoActionBar.DARK,
    };

    public static int getThemeNoActionBarRes(int theme) {
        if (theme < 0 || theme >= themesNoActionBar.length) {
            theme = 0;
        }
        return themesNoActionBar[theme];
    }


    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean getBoolean(Context context, String key) {
        switch (key) {
            case EASTEREGG:
                return getSharedPreferences(context).getBoolean(EASTEREGG, false);
            default:
                Log.e(TAG, "getBoolean: unknown key " + key);
                return false;
        }
    }

    public static int getInt(Context context, String key) {
        switch (key) {
            case THEME:
                return getIntFromStringPref(context, THEME, 0);
            default:
                Log.e(TAG, "getInt: unknown key " + key);
                return 0;
        }
    }

    private static int getIntFromStringPref(Context context, String key, int defaultValue) {
        try {
            return Integer.parseInt(getSharedPreferences(context)
                    .getString(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            Log.e(TAG, "getIntFromStringPref for key \"" + key + "\": " + e);
            return defaultValue;
        }
    }

    public static long getLong(Context context, String key) {
        switch (key) {
            case DEFAULT_FILTER:
                return getLongFromStringPref(context, DEFAULT_FILTER, -1L);
            default:
                Log.e(TAG, "getInt: unknown key " + key);
                return 0;
        }
    }

    private static long getLongFromStringPref(Context context, String key, long defaultValue) {
        try {
            return Long.parseLong(getSharedPreferences(context)
                    .getString(key, Long.toString(defaultValue)));
        } catch (NumberFormatException e) {
            Log.e(TAG, "getLongFromStringPref for key \"" + key + "\": " + e);
            return defaultValue;
        }
    }

    public static String getString(Context context, String key) {
        switch (key) {
            case DATE_FORMAT_OVERVIEW_1:
            case DATE_FORMAT_OVERVIEW_2:
            case DATE_FORMAT_OVERVIEW_3:
            case DATE_FORMAT_FULL_DATE_TIME:
            case DATE_FORMAT_DATE_FOR_TITLE:
                return getSharedPreferences(context).getString(key, "");
            default:
                Log.e(TAG, "getString: unknown key " + key);
                return null;
        }
    }

    public static void put(Context context, String key, boolean value) {
        getSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    public static void put(Context context, String key, int value) {
        if (THEME.equals(key)) {
            put(context, key, String.valueOf(value));
        } else {
            getSharedPreferences(context).edit().putInt(key, value).apply();
        }
    }

    public static void put(Context context, String key, String value) {
        getSharedPreferences(context).edit().putString(key, value).apply();
    }
}
