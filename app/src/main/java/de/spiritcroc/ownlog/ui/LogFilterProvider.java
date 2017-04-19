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

package de.spiritcroc.ownlog.ui;

import java.util.ArrayList;

import de.spiritcroc.ownlog.data.LogFilter;

/**
 * Loads log filters and uses them. Active filter can be selected by a {@link LogFilterSelector}
 */

public interface LogFilterProvider {
    void setFilterSelector(LogFilterSelector selector);
    void selectFilter(int position);
    ArrayList<LogFilter> getAvailableLogFilters();
    int getCurrentLogFilterSelection();
}
