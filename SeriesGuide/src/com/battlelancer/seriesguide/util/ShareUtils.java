
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.x.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.getglueapi.PrepareRequestTokenActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowInfoActivity;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

public class ShareUtils {

    public static final String KEY_GETGLUE_COMMENT = "com.battlelancer.seriesguide.getglue.comment";

    public static final String KEY_GETGLUE_IMDBID = "com.battlelancer.seriesguide.getglue.imdbid";

    protected static final String TAG = "ShareUtils";

    /**
     * Show a dialog allowing to chose from various sharing options.
     * 
     * @param shareData - a {@link Bundle} including all
     *            {@link ShareUtils.ShareItems}
     */
    public static void showShareDialog(FragmentManager manager, Bundle shareData) {
        // Create and show the dialog.
        ShareDialogFragment newFragment = ShareDialogFragment.newInstance(shareData);
        FragmentTransaction ft = manager.beginTransaction();
        newFragment.show(ft, "sharedialog");
    }

    public static class ShareDialogFragment extends DialogFragment {
        public static ShareDialogFragment newInstance(Bundle shareData) {
            ShareDialogFragment f = new ShareDialogFragment();
            f.setArguments(shareData);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String imdbId = getArguments().getString(ShareUtils.ShareItems.IMDBID);
            final String sharestring = getArguments().getString(ShareUtils.ShareItems.SHARESTRING);
            final CharSequence[] items = getResources().getStringArray(R.array.share_items);

            return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.share))
                    .setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            switch (item) {
                                case 0:
                                    // GetGlue check in
                                    if (imdbId.length() != 0) {
                                        showGetGlueDialog(getSupportFragmentManager(),
                                                getArguments());
                                    } else {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.noIMDBentry), Toast.LENGTH_LONG)
                                                .show();
                                    }
                                    break;
                                case 1: {
                                    // trakt check in

                                    // DialogFragment.show() will take care of
                                    // adding the fragment
                                    // in a transaction. We also want to remove
                                    // any currently showing
                                    // dialog, so make our own transaction and
                                    // take care of that here.
                                    FragmentTransaction ft = getFragmentManager()
                                            .beginTransaction();
                                    Fragment prev = getFragmentManager().findFragmentByTag(
                                            "progress-dialog");
                                    if (prev != null) {
                                        ft.remove(prev);
                                    }
                                    ft.addToBackStack(null);

                                    // Create and show the dialog.
                                    ProgressDialog newFragment = ProgressDialog.newInstance();

                                    // start the trakt check in task, add the
                                    // dialog as listener
                                    getArguments().putInt(ShareItems.TRAKTACTION,
                                            TraktAction.CHECKIN_EPISODE.index());
                                    new TraktTask(getActivity(), getFragmentManager(),
                                            getArguments(), newFragment).execute();

                                    newFragment.show(ft, "progress-dialog");
                                    break;
                                }
                                case 2: {
                                    // trakt mark as seen
                                    getArguments().putInt(ShareItems.TRAKTACTION,
                                            TraktAction.SEEN_EPISODE.index());
                                    new TraktTask(getActivity(), getSupportFragmentManager(),
                                            getArguments()).execute();
                                    break;
                                }
                                case 3: {
                                    // trakt rate
                                    getArguments().putInt(ShareItems.TRAKTACTION,
                                            TraktAction.RATE_EPISODE.index());
                                    TraktRateDialogFragment newFragment = TraktRateDialogFragment
                                            .newInstance(getArguments());
                                    FragmentTransaction ft = getSupportFragmentManager()
                                            .beginTransaction();
                                    newFragment.show(ft, "traktratedialog");
                                    break;
                                }
                                case 4: {
                                    // Android apps
                                    String text = sharestring;
                                    if (imdbId.length() != 0) {
                                        text += " " + ShowInfoActivity.IMDB_TITLE_URL + imdbId;
                                    }

                                    Intent i = new Intent(Intent.ACTION_SEND);
                                    i.setType("text/plain");
                                    i.putExtra(Intent.EXTRA_TEXT, text);
                                    startActivity(Intent.createChooser(i,
                                            getString(R.string.share_episode)));
                                    break;
                                }
                            }
                        }
                    }).create();
        }
    }

    public static void showGetGlueDialog(FragmentManager manager, Bundle shareData) {
        // Create and show the dialog.
        GetGlueDialogFragment newFragment = GetGlueDialogFragment.newInstance(shareData);
        FragmentTransaction ft = manager.beginTransaction();
        newFragment.show(ft, "getgluedialog");
    }

    public static class GetGlueDialogFragment extends DialogFragment {

        public static GetGlueDialogFragment newInstance(Bundle shareData) {
            GetGlueDialogFragment f = new GetGlueDialogFragment();
            f.setArguments(shareData);
            return f;
        }

        private EditText input;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String episodestring = getArguments().getString(ShareItems.EPISODESTRING);
            final String imdbId = getArguments().getString(ShareItems.IMDBID);

            input = new EditText(getActivity());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setMinLines(3);
            input.setGravity(Gravity.TOP);
            input.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(140)
            });

            if (savedInstanceState != null) {
                input.setText(savedInstanceState.getString("inputtext"));
            } else {
                input.setText(episodestring + ". #SeriesGuide");
            }

            return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.comment))
                    .setView(input)
                    .setPositiveButton(R.string.checkin, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onGetGlueCheckIn(getActivity(), input.getText().toString(), imdbId);
                        }
                    }).setNegativeButton(android.R.string.cancel, null).create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("inputtext", input.getText().toString());
        }
    }

    public static void onGetGlueCheckIn(final Activity activity, final String comment,
            final String imdbId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity
                .getApplicationContext());

        if (GetGlue.isAuthenticated(prefs)) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        GetGlue.checkIn(prefs, imdbId, comment);
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity,
                                        activity.getString(R.string.checkinsuccess),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        AnalyticsUtils.getInstance(activity).trackEvent("Sharing", "GetGlue",
                                "Success", 0);

                    } catch (final Exception e) {
                        Log.e(TAG, "GetGlue Check-In failed");
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(
                                        activity,
                                        activity.getString(R.string.checkinfailed) + " - "
                                                + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                        AnalyticsUtils.getInstance(activity).trackEvent("Sharing", "GetGlue",
                                "Failed", 0);

                    }
                }
            }).start();
        } else {
            Intent i = new Intent(activity, PrepareRequestTokenActivity.class);
            i.putExtra(ShareUtils.KEY_GETGLUE_IMDBID, imdbId);
            i.putExtra(ShareUtils.KEY_GETGLUE_COMMENT, comment);
            activity.startActivity(i);
        }
    }

    public interface ShareItems {
        String SEASON = "season";

        String IMDBID = "imdbId";

        String SHARESTRING = "sharestring";

        String EPISODESTRING = "episodestring";

        String EPISODE = "episode";

        String TVDBID = "tvdbid";

        String RATING = "rating";

        String TRAKTACTION = "traktaction";
    }

    public static String onCreateShareString(Context context, final Cursor episode) {
        String season = episode.getString(episode.getColumnIndexOrThrow(Episodes.SEASON));
        String number = episode.getString(episode.getColumnIndexOrThrow(Episodes.NUMBER));
        String title = episode.getString(episode.getColumnIndexOrThrow(Episodes.TITLE));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Utils.getNextEpisodeString(prefs, season, number, title);
    }

    public static void onAddCalendarEvent(Context context, String title, String description,
            String airdate, long airtime, String runtime) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("title", title);
        intent.putExtra("description", description);

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                    .getApplicationContext());
            boolean useUserTimeZone = prefs.getBoolean(SeriesGuidePreferences.KEY_USE_MY_TIMEZONE,
                    false);

            Calendar cal = Utils.getLocalCalendar(airdate, airtime, useUserTimeZone, context);

            long startTime = cal.getTimeInMillis();
            long endTime = startTime + Long.valueOf(runtime) * 60 * 1000;
            intent.putExtra("beginTime", startTime);
            intent.putExtra("endTime", endTime);

            context.startActivity(intent);

            AnalyticsUtils.getInstance(context).trackEvent("Sharing", "Calendar", "Success", 0);
        } catch (Exception e) {
            AnalyticsUtils.getInstance(context).trackEvent("Sharing", "Calendar", "Failed", 0);
            Toast.makeText(context, context.getString(R.string.addtocalendar_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static String toSHA1(byte[] convertme) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return byteArrayToHexString(md.digest(convertme));
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public enum TraktAction {
        SEEN_EPISODE(0), RATE_EPISODE(1), CHECKIN_EPISODE(2);

        final private int mIndex;

        private TraktAction(int index) {
            mIndex = index;
        }

        public int index() {
            return mIndex;
        }
    }

    public interface TraktStatus {
        String SUCCESS = "success";

        String FAILURE = "failure";
    }

    public static class TraktTask extends AsyncTask<Void, Void, Response> {

        private final Context mContext;

        private final FragmentManager mManager;

        private final Bundle mTraktData;

        private OnTaskFinishedListener mListener;

        public interface OnTaskFinishedListener {
            public void onTaskFinished();
        }

        /**
         * Do the specified TraktAction. traktData should include all required
         * parameters.
         * 
         * @param context
         * @param manager
         * @param traktData
         * @param action
         */
        public TraktTask(Context context, FragmentManager manager, Bundle traktData) {
            mContext = context;
            mManager = manager;
            mTraktData = traktData;
        }

        /**
         * Specify a listener which will be notified once any activity context
         * dependent work is completed.
         * 
         * @param context
         * @param manager
         * @param traktData
         * @param listener
         */
        public TraktTask(Context context, FragmentManager manager, Bundle traktData,
                OnTaskFinishedListener listener) {
            this(context, manager, traktData);
            mListener = listener;
        }

        @Override
        protected Response doInBackground(Void... params) {
            if (!isTraktCredentialsValid(mContext)) {
                // return null, so onPostExecute displays a credentials dialog
                // which later calls us again.
                return null;
            }

            ServiceManager manager;
            try {
                manager = Utils.getServiceManagerWithAuth(mContext, false);
            } catch (Exception e) {
                // password could not be decrypted
                Response r = new Response();
                r.status = TraktStatus.FAILURE;
                r.error = mContext.getString(R.string.trakt_decryptfail);
                return r;
            }

            // get some values
            final int tvdbid = mTraktData.getInt(ShareItems.TVDBID);
            final int season = mTraktData.getInt(ShareItems.SEASON);
            final int episode = mTraktData.getInt(ShareItems.EPISODE);
            final TraktAction action = TraktAction.values()[mTraktData
                    .getInt(ShareItems.TRAKTACTION)];

            // last chance to abort (return value thrown away)
            if (isCancelled()) {
                return null;
            }

            try {
                Response r = null;
                switch (action) {
                    case CHECKIN_EPISODE: {
                        r = manager.showService().checkin(tvdbid).season(season).episode(episode)
                                .fire();
                        break;
                    }
                    case SEEN_EPISODE: {
                        manager.showService().episodeSeen(tvdbid).episode(season, episode).fire();
                        r = new Response();
                        r.status = TraktStatus.SUCCESS;
                        r.message = mContext.getString(R.string.trakt_seen);
                        break;
                    }
                    case RATE_EPISODE: {
                        final Rating rating = Rating.fromValue(mTraktData
                                .getString(ShareItems.RATING));
                        r = manager.rateService().episode(tvdbid).season(season).episode(episode)
                                .rating(rating).fire();
                        break;
                    }
                }

                return r;
            } catch (TraktException te) {
                Response r = new Response();
                r.status = TraktStatus.FAILURE;
                r.error = te.getMessage();
                return r;
            } catch (ApiException e) {
                Response r = new Response();
                r.status = TraktStatus.FAILURE;
                r.error = e.getMessage();
                return r;
            }
        }

        @Override
        protected void onPostExecute(Response r) {
            if (r != null) {
                if (r.status.equalsIgnoreCase(TraktStatus.SUCCESS)) {
                    // all good
                    Toast.makeText(mContext,
                            mContext.getString(R.string.trakt_success) + ": " + r.message,
                            Toast.LENGTH_SHORT).show();
                } else if (r.status.equalsIgnoreCase(TraktStatus.FAILURE)) {
                    if (r.wait != 0) {
                        // looks like a check in is in progress
                        TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                                .newInstance(mTraktData, r.wait);
                        FragmentTransaction ft = mManager.beginTransaction();
                        newFragment.show(ft, "cancel-checkin-dialog");
                    } else {
                        // well, something went wrong
                        Toast.makeText(mContext,
                                mContext.getString(R.string.trakt_error) + ": " + r.error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // credentials are invalid
                TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                        .newInstance(mTraktData);
                FragmentTransaction ft = mManager.beginTransaction();
                newFragment.show(ft, "traktdialog");
            }

            // tell a potential listener that our work is done
            if (mListener != null) {
                mListener.onTaskFinished();
            }
        }
    }

    public static class TraktCredentialsDialogFragment extends DialogFragment {

        private boolean isForwardingGivenTask;

        public static TraktCredentialsDialogFragment newInstance(Bundle traktData) {
            TraktCredentialsDialogFragment f = new TraktCredentialsDialogFragment();
            f.setArguments(traktData);
            f.isForwardingGivenTask = true;
            return f;
        }

        public static TraktCredentialsDialogFragment newInstance() {
            TraktCredentialsDialogFragment f = new TraktCredentialsDialogFragment();
            f.isForwardingGivenTask = false;
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity().getApplicationContext();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.trakt_credentials_dialog, null);
            final FragmentManager fm = getSupportFragmentManager();
            final Bundle args = getArguments();

            // restore the username from settings
            final String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
            ((EditText) layout.findViewById(R.id.username)).setText(username);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(layout);
            builder.setTitle("trakt.tv");

            final View mailviews = layout.findViewById(R.id.mailviews);
            mailviews.setVisibility(View.GONE);

            ((CheckBox) layout.findViewById(R.id.checkNewAccount))
                    .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                mailviews.setVisibility(View.VISIBLE);
                            } else {
                                mailviews.setVisibility(View.GONE);
                            }
                        }
                    });

            builder.setPositiveButton(R.string.save, new OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    final String username = ((EditText) layout.findViewById(R.id.username))
                            .getText().toString();
                    final String passwordHash = ShareUtils.toSHA1(((EditText) layout
                            .findViewById(R.id.password)).getText().toString().getBytes());
                    final String email = ((EditText) layout.findViewById(R.id.email)).getText()
                            .toString();
                    final boolean isNewAccount = ((CheckBox) layout
                            .findViewById(R.id.checkNewAccount)).isChecked();

                    AsyncTask<String, Void, Response> accountValidatorTask = new AsyncTask<String, Void, Response>() {

                        @Override
                        protected Response doInBackground(String... params) {
                            // SHA of any password is always non-empty
                            if (username.length() == 0) {
                                return null;
                            }

                            // use a separate ServiceManager here to avoid
                            // setting wrong credentials
                            final ServiceManager manager = new ServiceManager();
                            manager.setApiKey(Constants.TRAKT_API_KEY);
                            manager.setAuthentication(username, passwordHash);
                            Response response = null;

                            try {
                                if (isNewAccount) {
                                    // create new account
                                    response = manager.accountService()
                                            .create(username, passwordHash, email).fire();
                                } else {
                                    // validate existing account
                                    response = manager.accountService().test().fire();
                                }
                            } catch (TraktException te) {
                                response = te.getResponse();
                            } catch (ApiException ae) {
                                response = null;
                            }

                            return response;
                        }

                        @Override
                        protected void onPostExecute(Response response) {
                            if (response != null) {
                                String passwordEncr;
                                // try to encrypt the password before storing it
                                try {
                                    passwordEncr = SimpleCrypto.encrypt(passwordHash, context);
                                } catch (Exception e) {
                                    passwordEncr = "";
                                }

                                // prepare writing credentials to settings
                                Editor editor = prefs.edit();
                                editor.putString(SeriesGuidePreferences.KEY_TRAKTUSER, username)
                                        .putString(SeriesGuidePreferences.KEY_TRAKTPWD,
                                                passwordEncr);

                                if (response.getStatus().equalsIgnoreCase("success")
                                        && passwordEncr.length() != 0 && editor.commit()) {
                                    // all went through
                                    Toast.makeText(context,
                                            response.getStatus() + ": " + response.getMessage(),
                                            Toast.LENGTH_SHORT).show();

                                    // set new auth data for service manager
                                    try {
                                        Utils.getServiceManagerWithAuth(context, true);
                                    } catch (Exception e) {
                                        // we don't care
                                    }
                                } else {
                                    Toast.makeText(context,
                                            response.getStatus() + ": " + response.getError(),
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(context,
                                        context.getString(R.string.trakt_generalerror),
                                        Toast.LENGTH_LONG).show();
                            }

                            if (isForwardingGivenTask) {
                                // relaunch the trakt task which called us
                                new TraktTask(context, fm, args).execute();
                            }
                        }
                    };

                    accountValidatorTask.execute();
                }
            });
            builder.setNegativeButton(R.string.dontsave, null);

            return builder.create();
        }
    }

    public static class TraktCancelCheckinDialogFragment extends DialogFragment {

        private int mWait;

        public static TraktCancelCheckinDialogFragment newInstance(Bundle traktData, int wait) {
            TraktCancelCheckinDialogFragment f = new TraktCancelCheckinDialogFragment();
            f.setArguments(traktData);
            f.mWait = wait;
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity().getApplicationContext();
            final FragmentManager fm = getSupportFragmentManager();
            final Bundle args = getArguments();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(context.getString(R.string.traktcheckin_inprogress,
                    DateUtils.formatElapsedTime(mWait)));

            builder.setPositiveButton(R.string.traktcheckin_cancel, new OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    AsyncTask<String, Void, Response> cancelCheckinTask = new AsyncTask<String, Void, Response>() {

                        @Override
                        protected Response doInBackground(String... params) {

                            ServiceManager manager;
                            try {
                                manager = Utils.getServiceManagerWithAuth(context, false);
                            } catch (Exception e) {
                                // password could not be decrypted
                                Response r = new Response();
                                r.status = TraktStatus.FAILURE;
                                r.error = context.getString(R.string.trakt_decryptfail);
                                return r;
                            }

                            Response response;
                            try {
                                response = manager.showService().cancelCheckin().fire();
                            } catch (TraktException te) {
                                Response r = new Response();
                                r.status = TraktStatus.FAILURE;
                                r.error = te.getMessage();
                                return r;
                            } catch (ApiException e) {
                                Response r = new Response();
                                r.status = TraktStatus.FAILURE;
                                r.error = e.getMessage();
                                return r;
                            }
                            return response;
                        }

                        @Override
                        protected void onPostExecute(Response r) {
                            if (r.status.equalsIgnoreCase(TraktStatus.SUCCESS)) {
                                // all good
                                Toast.makeText(
                                        context,
                                        context.getString(R.string.trakt_success) + ": "
                                                + r.message, Toast.LENGTH_SHORT).show();

                                // relaunch the trakt task which called us to
                                // try the check in again
                                new TraktTask(context, fm, args).execute();
                            } else if (r.status.equalsIgnoreCase(TraktStatus.FAILURE)) {
                                // well, something went wrong
                                Toast.makeText(context,
                                        context.getString(R.string.trakt_error) + ": " + r.error,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    cancelCheckinTask.execute();
                }
            });
            builder.setNegativeButton(R.string.traktcheckin_wait, null);

            return builder.create();
        }
    }

    public static class TraktRateDialogFragment extends DialogFragment {

        public static TraktRateDialogFragment newInstance(Bundle traktData) {
            TraktRateDialogFragment f = new TraktRateDialogFragment();
            f.setArguments(traktData);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            AlertDialog.Builder builder;

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.trakt_rate_dialog, null);
            final Button totallyNinja = (Button) layout.findViewById(R.id.totallyninja);
            final Button weakSauce = (Button) layout.findViewById(R.id.weaksauce);

            totallyNinja.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    final Rating rating = Rating.Love;
                    getArguments().putString(ShareItems.RATING, rating.toString());
                    new TraktTask(context, getSupportFragmentManager(), getArguments()).execute();
                    dismiss();
                }
            });

            weakSauce.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    final Rating rating = Rating.Hate;
                    getArguments().putString(ShareItems.RATING, rating.toString());
                    new TraktTask(context, getSupportFragmentManager(), getArguments()).execute();
                    dismiss();
                }
            });

            builder = new AlertDialog.Builder(context);
            builder.setView(layout);
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    public static class ProgressDialog extends DialogFragment implements
            TraktTask.OnTaskFinishedListener {

        public static ProgressDialog newInstance() {
            ProgressDialog f = new ProgressDialog();
            f.setCancelable(false);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setStyle(STYLE_NO_TITLE, 0);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.progress_dialog, container, false);
            return v;
        }

        @Override
        public void onTaskFinished() {
            dismiss();
        }
    }

    public static boolean isTraktCredentialsValid(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, "");

        return (!username.equalsIgnoreCase("") && !password.equalsIgnoreCase(""));
    }
}
