/*
 * Copyright (C) 2018 SpiritCroc
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

package de.spiritcroc.ownlog.data;

public class ImportItemInfo {
    public enum ChangeStatus {SAME, NEW, CHANGED, UPDATED, DEPRECATED,};
    public enum Strategy {GLOBAL, LATEST, OVERWRITE, KEEP, IF_NEW,};

    private ChangeStatus mChangeStatus;
    private Strategy mStrategy;

    public ImportItemInfo(ChangeStatus changeStatus) {
        mChangeStatus = changeStatus;
        mStrategy = Strategy.GLOBAL;
    }

    public ChangeStatus getChangeStatus() {
        return mChangeStatus;
    }

    public Strategy getStrategy() {
        return mStrategy;
    }

    public void setStrategy(Strategy strategy) {
        mStrategy = strategy;
    }
}
