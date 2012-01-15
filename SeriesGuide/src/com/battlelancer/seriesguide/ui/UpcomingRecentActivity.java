
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.donate.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.thetvdbapi.SearchResult;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

public class UpcomingRecentActivity extends BaseActivity implements OnAddShowListener {
    ViewPager mViewPager;

    TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upcoming_multipane);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab upcomingTab = actionBar.newTab().setText(R.string.upcoming);
        ActionBar.Tab recentTab = actionBar.newTab().setText(R.string.recent);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, actionBar, mViewPager);
        // upcoming tab
        final Bundle argsUpcoming = new Bundle();
        argsUpcoming.putString("query", Episodes.FIRSTAIRED + ">=?");
        argsUpcoming.putString("sortorder", Episodes.FIRSTAIRED + " ASC," + Shows.AIRSTIME
                + " ASC," + Shows.TITLE + " ASC");
        argsUpcoming.putString("analyticstag", "/Upcoming");
        argsUpcoming.putInt("loaderid", 10);
        mTabsAdapter.addTab(upcomingTab, UpcomingFragment.class, argsUpcoming);

        // recent tab
        final Bundle argsRecent = new Bundle();
        argsRecent.putString("query", Episodes.FIRSTAIRED + "<?");
        argsRecent.putString("sortorder", Episodes.FIRSTAIRED + " DESC," + Shows.AIRSTIME + " ASC,"
                + Shows.TITLE + " ASC");
        argsRecent.putString("analyticstag", "/Recent");
        argsUpcoming.putInt("loaderid", 20);
        mTabsAdapter.addTab(recentTab, UpcomingFragment.class, argsRecent);

        // trakt friends tab
        ActionBar.Tab friendsTab = actionBar.newTab().setText(R.string.friends);
        mTabsAdapter.addTab(friendsTab, TraktFriendsFragment.class, null);

        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt("index"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final Context mContext;

        private final ActionBar mActionBar;

        private final ViewPager mViewPager;

        private final ArrayList<String> mTabs = new ArrayList<String>();

        private final ArrayList<Bundle> mArgs = new ArrayList<Bundle>();

        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            mTabs.add(clss.getName());
            mArgs.add(args);
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(mContext, mTabs.get(position), mArgs.get(position));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }
    }

    /**
     * Provide a listener for the TraktFriendsFragment.
     */
    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance(this).performAddTask(show);
    }
}
