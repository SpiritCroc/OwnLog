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

package de.spiritcroc.ownlog;

import android.content.Context;
import android.support.annotation.StringRes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class DateFormatter {

    public static String getOverviewPart1(Context context, long millis) {
        return getDateFormatted(context, millis, Settings.DATE_FORMAT_OVERVIEW_1,
                R.string.date_format_overview_1);
    }

    public static String getOverviewPart2(Context context, long millis) {
        return getDateFormatted(context, millis, Settings.DATE_FORMAT_OVERVIEW_2,
                R.string.date_format_overview_2);
    }

    public static String getOverviewPart3(Context context, long millis) {
        return getDateFormatted(context, millis, Settings.DATE_FORMAT_OVERVIEW_3,
                R.string.date_format_overview_3);
    }

    public static String getFullDateTime(Context context, long millis) {
        return getDateFormatted(context, millis, Settings.DATE_FORMAT_FULL_DATE_TIME,
                R.string.date_format_full_date_time);
    }

    public static String getDateForTitle(Context context, long millis) {
        return getDateFormatted(context, millis, Settings.DATE_FORMAT_DATE_FOR_TITLE,
                R.string.date_format_date_for_title);
    }

    private static String getDateFormatted(Context context, long millis, String settingKey,
                                           @StringRes int defaultRes) {
        String format = Settings.getString(context, settingKey);
        if ("".equals(format)) {
            format = context.getString(defaultRes);
        }
        try {
            return getDate(millis, new SimpleDateFormat(format));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // Resort to default
            return getDate(millis, new SimpleDateFormat(context.getString(defaultRes)));
        }
    }

    private static String getDate(long millis, DateFormat format) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return format.format(c.getTime());
    }

    public static ArrayList<Integer> getNewPart1Indexes(Context context, DateProvider[] dates) {
        ArrayList<Integer> result = new ArrayList<>();
        if (dates.length == 0) {
            return result;
        }
        result.add(0);
        String previous = getOverviewPart1(context, dates[0].getDate());
        for (int i = 1; i < dates.length; i++) {
            String next = getOverviewPart1(context, dates[i].getDate());
            if (next.equals(previous)) {
                continue;
            }
            result.add(i);
            previous = next;
        }
        return result;
    }

    public interface DateProvider {
        long getDate();
    }

    public static String getAutoDateFormatted(long millis) {
        return getDate(millis, SimpleDateFormat.getDateInstance());
    }
}
