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
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import de.spiritcroc.ownlog.Constants;
import de.spiritcroc.ownlog.R;
import de.spiritcroc.ownlog.ui.fragment.BaseFragment;
import de.spiritcroc.ownlog.ui.fragment.LogFragment;

public class SingleFragmentActivity extends BaseActivity {

    private static final String FRAGMENT_TAG = SingleFragmentActivity.class.getName() + ".Fragment";

    private static final String KEY_SHOULD_ENABLE_HOME_AS_UP =
            SingleFragmentActivity.class.getName() + ".shouldEnableHomeAsUp";

    protected boolean mShouldEnableHomeAsUp = false;

    private BaseFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_toolbar);

        setSupportActionBar(((Toolbar) findViewById(R.id.toolbar)));

        mFragment = (BaseFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (mFragment == null) {
            // Create new fragment
            mFragment = getNewFragment();
            getFragmentManager().beginTransaction().replace(R.id.content, mFragment, FRAGMENT_TAG)
                    .commit();
        }
        setupFragment(mFragment);

        if (savedInstanceState != null) {
            mShouldEnableHomeAsUp = savedInstanceState.getBoolean(KEY_SHOULD_ENABLE_HOME_AS_UP,
                    mShouldEnableHomeAsUp);
        }

        setTitle(getNewTitle());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(mShouldEnableHomeAsUp);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SHOULD_ENABLE_HOME_AS_UP, mShouldEnableHomeAsUp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!mFragment.onUpOrBackPressed()) {
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!mFragment.onUpOrBackPressed()) {
            super.onBackPressed();
        }
    }

    protected BaseFragment getNewFragment() {
        String fragmentClassExtra = getIntent().getStringExtra(Constants.EXTRA_FRAGMENT_CLASS);
        BaseFragment fragment = null;
        if (fragmentClassExtra != null  && !fragmentClassExtra.isEmpty()) {
            try {
                Class<?> fragmentClass = Class.forName(fragmentClassExtra);
                fragment = (BaseFragment) fragmentClass.newInstance();
                mShouldEnableHomeAsUp = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fragment == null) {
            fragment = new LogFragment();
            mShouldEnableHomeAsUp = false;
        }
        fragment.setArguments(getIntent().getBundleExtra(Constants.EXTRA_FRAGMENT_BUNDLE));
        return fragment;
    }

    /**
     * Allows setting listeners etc. in child classes
     *
     * @param fragment
     * The fragment returned by getNewFragment(), or a reused one (after configuration change)
     */
    protected void setupFragment(BaseFragment fragment) {}

    protected String getNewTitle() {
        String title = getIntent().getStringExtra(Constants.EXTRA_TITLE);
        if (title == null  || title.isEmpty()) {
            title = getString(R.string.app_name);
        }
        return title;
    }
}
