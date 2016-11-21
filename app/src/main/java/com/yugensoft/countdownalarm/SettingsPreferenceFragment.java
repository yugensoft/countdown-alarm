package com.yugensoft.countdownalarm;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.util.Set;


public class SettingsPreferenceFragment extends PreferenceFragment {
    public SettingsPreferenceFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // construct the preferencefragment
        addPreferencesFromResource(R.xml.preferences_settings);

        final NumberPickerPreference prefSnoozeTime = (NumberPickerPreference) getPreferenceManager().findPreference(getString(R.string.pref_snooze_duration));
        String minutes = String.valueOf(SettingsActivity.getSnoozeDuration(getActivity()));
        prefSnoozeTime.setSummary(minutes + " " + getString(R.string.minutes));
        prefSnoozeTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int minutes = (int)(newValue);
                prefSnoozeTime.setSummary(String.valueOf(minutes) + " " + getString(R.string.minutes));
                return true;
            }
        });

    }
}
