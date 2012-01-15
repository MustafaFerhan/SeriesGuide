
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.donate.R;
import com.battlelancer.seriesguide.WatchedBox;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Utils;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class UpcomingFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MARK_WATCHED_ID = 0;

    private static final int MARK_UNWATCHED_ID = 1;

    private SimpleCursorAdapter mAdapter;

    private boolean mDualPane;

    static UpcomingFragment newInstance(String query, String sortOrder, String analyticsTag,
            int loaderId) {
        UpcomingFragment f = new UpcomingFragment();
        Bundle args = new Bundle();
        args.putString("query", query);
        args.putString("sortorder", sortOrder);
        args.putString("analyticstag", analyticsTag);
        args.putInt("loaderid", loaderId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setSelector(R.drawable.list_selector_holo_dark);

        setEmptyText(getString(R.string.noupcoming));

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFragment = getActivity().findViewById(R.id.fragment_details);
        mDualPane = detailsFragment != null && detailsFragment.getVisibility() == View.VISIBLE;

        setupAdapter();

        getSupportActivity().getSupportLoaderManager().initLoader(
                getArguments().getInt("loaderid"), null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        final String tag = getArguments().getString("analyticstag");
        AnalyticsUtils.getInstance(getActivity()).trackPageView(tag);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // only display the action appropiate for the items current state
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        WatchedBox watchedBox = (WatchedBox) info.targetView.findViewById(R.id.watchedBoxUpcoming);
        if (watchedBox.isChecked()) {
            menu.add(0, MARK_UNWATCHED_ID, 1, R.string.unmark_episode);
        } else {
            menu.add(0, MARK_WATCHED_ID, 0, R.string.mark_episode);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case MARK_WATCHED_ID:
                DBUtils.markEpisode(getActivity(), String.valueOf(info.id), true);
                return true;

            case MARK_UNWATCHED_ID:
                DBUtils.markEpisode(getActivity(), String.valueOf(info.id), false);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void setupAdapter() {

        String[] from = new String[] {
                Episodes.TITLE, Episodes.WATCHED, Episodes.NUMBER, Episodes.FIRSTAIRED,
                Shows.TITLE, Shows.NETWORK
        };
        int[] to = new int[] {
                R.id.textViewUpcomingEpisode, R.id.watchedBoxUpcoming, R.id.textViewUpcomingNumber,
                R.id.textViewUpcomingAirdate, R.id.textViewUpcomingShow,
                R.id.textViewUpcomingNetwork
        };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.upcoming_row, null, from, to, 0);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, final Cursor cursor, int columnIndex) {
                // set WatchedBox state and add onClickListener
                if (columnIndex == UpcomingQuery.WATCHED) {
                    WatchedBox wb = (WatchedBox) view;

                    // save rowid to hand over to OnClick event listener
                    final String rowid = cursor.getString(UpcomingQuery._ID);

                    wb.setOnClickListener(new OnClickListener() {

                        public void onClick(View v) {
                            ((WatchedBox) v).toggle();
                            DBUtils.markEpisode(getActivity(), rowid,
                                    ((WatchedBox) v).isChecked());
                        }
                    });

                    wb.setChecked(cursor.getInt(columnIndex) > 0);
                    return true;
                } else if (columnIndex == UpcomingQuery.NUMBER) {
                    // set season and episode number
                    TextView text = (TextView) view;
                    String episodeString = cursor.getString(UpcomingQuery.SEASON);
                    String number = cursor.getString(UpcomingQuery.NUMBER);
                    if (number.length() == 1) {
                        episodeString += "x0" + number;
                    } else {
                        episodeString += "x" + number;
                    }
                    text.setText(episodeString);
                    return true;
                } else if (columnIndex == UpcomingQuery.FIRSTAIRED) {
                    TextView tv = (TextView) view;

                    // set airdate
                    String fieldValue = cursor.getString(UpcomingQuery.FIRSTAIRED);
                    if (fieldValue.length() != 0) {
                        tv.setText(Utils.parseDateToLocalRelative(fieldValue,
                                cursor.getLong(UpcomingQuery.SHOW_AIRSTIME), getActivity()));
                    } else {
                        tv.setText("");
                    }

                    return true;
                } else if (columnIndex == UpcomingQuery.SHOW_NETWORK) {
                    TextView tv = (TextView) view;
                    String fieldValue = "";

                    // add airtime
                    long airtime = cursor.getLong(UpcomingQuery.SHOW_AIRSTIME);
                    String value = Utils.parseMillisecondsToTime(airtime, null,
                            getActivity())[0];
                    if (value.length() != 0) {
                        fieldValue += value + " ";
                    }
                    // add network
                    value = cursor.getString(UpcomingQuery.SHOW_NETWORK);
                    if (value.length() != 0) {
                        fieldValue += getString(R.string.show_network) + " " + value;
                    }

                    tv.setText(fieldValue);
                    return true;
                }
                return false;
            }
        });

        setListAdapter(mAdapter);

        final ListView list = getListView();
        list.setFastScrollEnabled(true);
        registerForContextMenu(list);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails(String.valueOf(id));
    }

    private void showDetails(String episodeId) {
        if (mDualPane) {
            // Check if fragment is shown, create new if needed.
            EpisodeDetailsFragment detailsFragment = (EpisodeDetailsFragment) getFragmentManager()
                    .findFragmentById(R.id.fragment_details);
            if (detailsFragment == null
                    || !detailsFragment.getEpisodeId().equalsIgnoreCase(episodeId)) {
                // Make new fragment to show this selection.
                detailsFragment = EpisodeDetailsFragment.newInstance(episodeId, true);

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_details, detailsFragment, "fragmentDetails");
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            Intent intent = new Intent();
            intent.setClass(getActivity(), EpisodeDetailsActivity.class);
            intent.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_ID, episodeId);
            startActivity(intent);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        SimpleDateFormat pdtformat = Constants.theTVDBDateFormat;
        pdtformat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        final Date date = new Date();
        final String today = pdtformat.format(date);
        final String query = getArguments().getString("query");
        final String sortOrder = getArguments().getString("sortorder");
        return new CursorLoader(getActivity(), Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, query, new String[] {
                    today
                }, sortOrder);
        // Episodes.FIRSTAIRED + ">=?"
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    interface UpcomingQuery {
        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.WATCHED,
                Episodes.NUMBER, Episodes.SEASON, Episodes.FIRSTAIRED, Shows.TITLE, Shows.AIRSTIME,
                Shows.NETWORK, Shows.POSTER
        };

        // String sortOrder = Episodes.FIRSTAIRED + " ASC," + Shows.AIRSTIME +
        // " ASC," + Shows.TITLE
        // + " ASC";

        int _ID = 0;

        int TITLE = 1;

        int WATCHED = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int FIRSTAIRED = 5;

        int SHOW_TITLE = 6;

        int SHOW_AIRSTIME = 7;

        int SHOW_NETWORK = 8;

        int SHOW_POSTER = 9;
    }
}
