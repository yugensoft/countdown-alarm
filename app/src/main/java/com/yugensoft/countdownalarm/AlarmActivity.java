package com.yugensoft.countdownalarm;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import butterknife.ButterKnife;

public class AlarmActivity extends AppCompatActivity {
    public static String KEY_ALARM_ID = "alarm-id";

    private long mAlarmId;
    private AlarmPreferencesFragment mFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        ButterKnife.bind(this);

        mAlarmId = getIntent().getExtras().getLong(KEY_ALARM_ID);

        // Insert the Preferences fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mFragment = AlarmPreferencesFragment.newInstance(mAlarmId);

        fragmentTransaction.add(R.id.ll_fragment,mFragment);
        fragmentTransaction.commit();


    }

    public void cancelAlarm(@Nullable View view) {
        finish();
    }

    public void saveAlarm(@Nullable View view) {
        mFragment.saveAlarm();

        finish();
    }
}
