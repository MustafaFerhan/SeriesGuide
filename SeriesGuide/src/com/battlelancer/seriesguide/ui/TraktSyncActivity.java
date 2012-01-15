
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.donate.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.TraktSync;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class TraktSyncActivity extends BaseActivity {

    private static final int DIALOG_SELECT_SHOWS = 100;

    private TraktSync mSyncTask;

    private CheckBox mSyncUnseenEpisodes;

    private View mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trakt_sync);

        mContainer = findViewById(R.id.syncbuttons);

        mSyncUnseenEpisodes = (CheckBox) findViewById(R.id.checkBoxSyncUnseen);
        final Button syncToDeviceButton = (Button) findViewById(R.id.syncToDeviceButton);
        syncToDeviceButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (mSyncTask == null
                        || (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.FINISHED)) {
                    mSyncTask = (TraktSync) new TraktSync(TraktSyncActivity.this, mContainer,
                            false, mSyncUnseenEpisodes.isChecked()).execute();
                }
            }
        });

        final Button syncToTraktButton = (Button) findViewById(R.id.syncToTraktButton);
        syncToTraktButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showDialog(DIALOG_SELECT_SHOWS);

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mSyncTask.cancel(true);
            mSyncTask = null;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SELECT_SHOWS:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.pref_traktsync);
                final Cursor shows = getContentResolver().query(Shows.CONTENT_URI, new String[] {
                        Shows._ID, Shows.TITLE, Shows.SYNCENABLED
                }, null, null, Shows.TITLE + " ASC");

                String[] showTitles = new String[shows.getCount()];
                boolean[] syncEnabled = new boolean[shows.getCount()];
                for (int i = 0; i < showTitles.length; i++) {
                    shows.moveToNext();
                    showTitles[i] = shows.getString(1);
                    syncEnabled[i] = shows.getInt(2) == 1;
                }

                builder.setMultiChoiceItems(showTitles, syncEnabled,
                        new OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                shows.moveToFirst();
                                shows.move(which);
                                final String showId = shows.getString(0);
                                final ContentValues values = new ContentValues();
                                values.put(Shows.SYNCENABLED, isChecked);
                                getContentResolver().update(Shows.buildShowUri(showId), values,
                                        null, null);
                            }
                        });
                builder.setPositiveButton(R.string.trakt_synctotrakt,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mSyncTask == null
                                        || (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.FINISHED)) {
                                    mSyncTask = (TraktSync) new TraktSync(TraktSyncActivity.this,
                                            mContainer, true, mSyncUnseenEpisodes.isChecked())
                                            .execute();
                                }
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);

                return builder.create();
        }
        return null;
    }
}
