package com.yugensoft.countdownalarm;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import butterknife.ButterKnife;

public class AlarmActivity extends AppCompatActivity implements AlarmPreferenceFragment.OnFragmentInteractionListener{
    public static String KEY_ALARM_ID = "alarm-id";

    public static int RES_CANCELED = 0;
    public static int RES_SAVED = 1;

    // loaded from intent
    private long mAlarmId;

    // main fragment
    private AlarmPreferenceFragment mFragment;

    public static Intent newIntent(Context context, long id){
        Intent intent = new Intent(context,AlarmActivity.class);
        intent.putExtra(AlarmActivity.KEY_ALARM_ID,id);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        ButterKnife.bind(this);

        mAlarmId = getIntent().getExtras().getLong(KEY_ALARM_ID);

        // Insert the Preferences fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mFragment = AlarmPreferenceFragment.newInstance(mAlarmId);

        fragmentTransaction.add(R.id.ll_fragment,mFragment);
        fragmentTransaction.commit();


    }

    public void cancelAlarm(@Nullable View view) {
        setResult(RES_CANCELED);
        finish();
    }

    public void saveAlarm(@Nullable View view) {
        mFragment.saveAlarm();

        Intent data = new Intent();
        data.putExtra(KEY_ALARM_ID,mAlarmId);
        setResult(RES_SAVED,data);
        finish();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // not used
    }
}
