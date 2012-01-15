
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.x.R;
import com.battlelancer.seriesguide.util.ShareUtils.TraktCredentialsDialogFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class WelcomeDialogFragment extends DialogFragment {

    public static WelcomeDialogFragment newInstance() {
        WelcomeDialogFragment f = new WelcomeDialogFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.welcome_dialog, container, false);
        v.findViewById(R.id.welcome_setuptrakt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                        .newInstance();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                newFragment.show(ft, "traktdialog");
            }
        });
        v.findViewById(R.id.welcome_add).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), AddActivity.class);
                startActivity(i);
                dismiss();
            }
        });
        return v;
    }
}
