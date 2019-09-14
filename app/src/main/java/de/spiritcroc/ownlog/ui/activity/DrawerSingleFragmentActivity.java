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

package de.spiritcroc.ownlog.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;

import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.Settings;
import de.spiritcroc.ownlog.data.LogFilter;
import de.spiritcroc.ownlog.ui.LogFilterProvider;
import de.spiritcroc.ownlog.ui.LogFilterSelector;
import de.spiritcroc.ownlog.ui.fragment.BaseFragment;
import de.spiritcroc.ownlog.ui.fragment.ExportDialog;
import de.spiritcroc.ownlog.ui.fragment.ImportLogFragment;
import de.spiritcroc.ownlog.ui.fragment.LogFilterEditFragment;
import de.spiritcroc.ownlog.ui.settings.AboutActivity;
import de.spiritcroc.ownlog.ui.settings.SettingsActivity;

public class DrawerSingleFragmentActivity extends SingleFragmentActivity
        implements LogFilterSelector {

    private static final String TAG = DrawerSingleFragmentActivity.class.getSimpleName();

    private static final int RESULT_CODE_IMPORT = 1;

    private Drawer mDrawer;

    private LogFilterProvider mFilterProvider;

    private static final int DRAWER_ID_SETTINGS = -100;
    private static final int DRAWER_ID_ABOUT = -101;
    private static final int DRAWER_ID_THEME = -102;
    private static final int DRAWER_ID_FILTER_ADD = -201;
    private static final int DRAWER_ID_EXPORT = -301;
    private static final int DRAWER_ID_IMPORT = -302;
    private int mFilterCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Toolbar is inherited from parent's layout
        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withSavedInstance(savedInstanceState)
                .withToolbar((Toolbar) findViewById(R.id.toolbar))
                .withOnDrawerItemClickListener(mDrawerItemClickListener)
                .withOnDrawerItemLongClickListener(mDrawerItemLongClickListener)
                .withRootView(R.id.drawer_layout)
                .withActionBarDrawerToggleAnimated(true)
                .withSelectedItemByPosition(-1)
                .withDisplayBelowStatusBar(false)
                .build();

        setupDrawer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(mDrawer.saveInstanceState(outState));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mDrawer.closeDrawer();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setFilterProvider(LogFilterProvider provider) {
        if (mFilterProvider != null) {
            mFilterProvider.setFilterSelector(null);
        }
        mFilterProvider = provider;
        if (provider != null) {
            mFilterProvider.setFilterSelector(this);
        }
    }

    @Override
    public void onFilterUpdate() {
        // Update drawer
        setupDrawer();
    }

    @Override
    public void overwriteFilterSelection(int position) {
        mDrawer.setSelection(position, true);
    }

    @Override
    protected void setupFragment(BaseFragment fragment) {
        if (fragment instanceof LogFilterProvider) {
            setFilterProvider((LogFilterProvider) fragment);
        } else {
            setFilterProvider(null);
        }
    }

    private void setupDrawer() {
        mDrawer.removeAllItems();
        mFilterCount = 0;
        if (mFilterProvider != null) {
            ArrayList<LogFilter> filters = mFilterProvider.getAvailableLogFilters();
            if (filters != null) {
                mDrawer.addItem(new SectionDrawerItem()
                        .withName(R.string.drawer_filters));
                for (LogFilter filter : filters) {
                    boolean selected =
                            mFilterCount == mFilterProvider.getCurrentLogFilterSelection();
                    mDrawer.addItem(new PrimaryDrawerItem()
                            .withIdentifier(mFilterCount)
                            .withSetSelected(selected)
                            .withIcon(R.drawable.ic_drawer_filter)
                            .withName(filter.name));
                    mFilterCount++;
                    if (selected) {
                        // Set title of activity
                        setTitle(filter.name);
                    }
                }
                mDrawer.addItem(new SecondaryDrawerItem()
                        .withIdentifier(DRAWER_ID_FILTER_ADD)
                        .withSelectable(false)
                        .withIcon(R.drawable.ic_drawer_add)
                        .withName(R.string.drawer_add_filter));
            }
        }
        mDrawer.addItem(new SectionDrawerItem()
                .withName(R.string.drawer_data_management));
        mDrawer.addItem(new SecondaryDrawerItem()
                .withIdentifier(DRAWER_ID_EXPORT)
                .withSelectable(false)
                .withIcon(R.drawable.ic_drawer_export)
                .withName(R.string.drawer_export));
        mDrawer.addItem(new SecondaryDrawerItem()
                .withIdentifier(DRAWER_ID_IMPORT)
                .withSelectable(false)
                .withIcon(R.drawable.ic_drawer_import)
                .withName(R.string.drawer_import));

        if (isEastereggEnabled()) {
            mDrawer.addItem(new SectionDrawerItem()
                    .withName(R.string.easteregg));
            mDrawer.addItem(new SecondaryDrawerItem()
                    .withIdentifier(DRAWER_ID_THEME)
                    .withSelectable(false)
                    .withIcon(R.drawable.ic_drawer_theme)
                    .withName(R.string.pref_theme_title));
        }

        mDrawer.addItem(new SectionDrawerItem()
                .withName(R.string.drawer_more));
        mDrawer.addItem(new SecondaryDrawerItem()
                .withIdentifier(DRAWER_ID_SETTINGS)
                .withSelectable(false)
                .withIcon(R.drawable.ic_drawer_settings)
                .withName(R.string.drawer_settings));
        mDrawer.addItem(new SecondaryDrawerItem()
                .withIdentifier(DRAWER_ID_ABOUT)
                .withSelectable(false)
                .withIcon(R.drawable.ic_drawer_about)
                .withName(R.string.drawer_about));
    }

    private Drawer.OnDrawerItemClickListener mDrawerItemClickListener =
            new Drawer.OnDrawerItemClickListener() {
                @Override
                public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                    boolean result = false;
                    long id = drawerItem.getIdentifier();
                    if (id >= 0 && id < mFilterCount) {
                        if (mFilterProvider == null) {
                            Log.e(TAG, "Selected a filter, but no provider available");
                        } else {
                            // id < mFilterCount, so converting to int is save
                            mFilterProvider.selectFilter((int) id);
                            // Set title of activity
                            setTitle(mFilterProvider.getAvailableLogFilters().get((int) id).name);
                        }
                        result = true;
                    } else if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
                        Log.w(TAG, "Selected a drawer item with unknown id " + id);
                    } else {
                        switch ((int) id) {
                            case DRAWER_ID_SETTINGS:
                                startActivity(new Intent(DrawerSingleFragmentActivity.this,
                                        SettingsActivity.class));
                                result = true;
                                break;
                            case DRAWER_ID_ABOUT:
                                startActivity(new Intent(DrawerSingleFragmentActivity.this,
                                        AboutActivity.class));
                                result = true;
                                break;
                            case DRAWER_ID_FILTER_ADD:
                                new LogFilterEditFragment()
                                        .show(getFragmentManager(), "LogFilterEditFragment");
                                result = true;
                                break;
                            case DRAWER_ID_EXPORT:
                                new ExportDialog().show(getFragmentManager(), "ExportDialog");
                                break;
                            case DRAWER_ID_IMPORT:
                                openImportChooser();
                                break;
                            case DRAWER_ID_THEME:
                                switchTheme();
                                result = true;
                                break;
                            default:
                                Log.w(TAG, "Selected a drawer item with unknown id " + id);
                                break;
                        }
                    }
                    if (result) {
                        mDrawer.closeDrawer();
                    }
                    return result;
                }
            };

    private Drawer.OnDrawerItemLongClickListener mDrawerItemLongClickListener =
            new Drawer.OnDrawerItemLongClickListener() {
                @Override
                public boolean onItemLongClick(View view, int position, IDrawerItem drawerItem) {
                    long id = drawerItem.getIdentifier();
                    if (id >= 0 && id < mFilterCount) {
                        if (mFilterProvider == null) {
                            Log.e(TAG, "Selected a filter, but no provider available");
                        } else {
                            // id < mFilterCount, so converting to int is save
                            LogFilter clickedFilter = mFilterProvider.getAvailableLogFilters()
                                    .get((int) id);
                            if (!clickedFilter.isDefaultFilter()) {
                                new LogFilterEditFragment().setEditItemId(clickedFilter.id)
                                        .show(getFragmentManager(),
                                        "LogFilterEditFragment");
                            }
                        }
                        return true;
                    }
                    return false;
                }
            };

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    private void openImportChooser() {
        // TODO what if none installed?
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, RESULT_CODE_IMPORT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_CODE_IMPORT:
                    if (data != null) {
                        ImportLogFragment.show(this, data.getData());
                    }
                    break;
            }
        }
    }

    /**
     * For debugging purposes
     */
    private void switchTheme() {
        Settings.put(this, Settings.THEME,
                (Settings.getInt(this, Settings.THEME) + 1) % Settings.themesNoActionBar.length);
        recreate();
    }
}
