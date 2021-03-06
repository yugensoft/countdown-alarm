package com.yugensoft.countdownalarm;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class SettingsActivity extends AppCompatActivity {
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsPreferenceFragment())
                .commit();

        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication)getApplication()).getDefaultTracker();
    }

    public static long getSnoozeDuration(Context context) {
        return (long)PreferenceManager.getDefaultSharedPreferences(context).getInt(
                context.getString(R.string.pref_snooze_duration),
                context.getResources().getInteger(R.integer.def_snooze_duration)
        );
    }
    public static long getSnoozeDurationInMs(Context context) {
        return getSnoozeDuration(context) * 60 * 1000;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
