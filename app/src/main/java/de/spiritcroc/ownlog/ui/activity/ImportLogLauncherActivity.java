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

package de.spiritcroc.ownlog.ui.activity;

import android.os.Bundle;

import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.ui.fragment.BaseFragment;
import de.spiritcroc.ownlog.ui.fragment.ImportLogFragment;

public class ImportLogLauncherActivity extends SingleFragmentActivity {

    @Override
    protected BaseFragment getNewFragment() {
        BaseFragment fragment = new ImportLogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ImportLogFragment.EXTRA_URI, getIntent().getData().toString());
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    protected String getNewTitle() {
        return getString(R.string.title_import_log);
    }
}
