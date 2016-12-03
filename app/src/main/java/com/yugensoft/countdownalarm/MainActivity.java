package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "app-debug";
    private static final boolean DEBUG_LICENSE_AGREE = false;

    private static int REQ_ALARM_ACTIVITY = 0;

    public static final String KEY_HAS_AGREED = "agreed";

    private Tracker mTracker;
    private DaoSession mDaoSession;
    private AlarmManager mAlarmManager;

    private AlarmListAdapter alarmListAdapter;
    private ListView alarmListView;
    private AdView mAdView;

    private Date mNextAlarm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if user as agreed to terms yet, and if not open that activity and close this one
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_HAS_AGREED,false)){
            startActivity(new Intent(this,LicenseAgreementActivity.class));
            finish();
        }
        if(DEBUG_LICENSE_AGREE){
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MainActivity.KEY_HAS_AGREED,false).apply();
        }

        // Get the shared tracker instance
        mTracker = ((CountdownAlarmApplication)getApplication()).getDefaultTracker();
        // get Dao
        mDaoSession = ((CountdownAlarmApplication) getApplication()).getDaoSession();
        // get views
        alarmListView = (ListView)findViewById(R.id.listview_alarms);

        // Alarm list
        alarmListAdapter = new AlarmListAdapter(
                this,
                mDaoSession
        );
        alarmListView.setAdapter(alarmListAdapter);
        alarmListView.setOnItemClickListener(this);

        // engage all alarms
        mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        AlarmFunctions.engageAllAlarms(this,mDaoSession,mAlarmManager);

        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication)getApplication()).getDefaultTracker();

        // The ad
        mAdView = (AdView) findViewById(R.id.adView);
        MiscFunctions.loadAdIntoAdView(mAdView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // ensure alarms list is up to date
        alarmListAdapter.updateAlarms();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.mi_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivityForResult(AlarmActivity.newIntent(this,id),REQ_ALARM_ACTIVITY);
    }

    /**
     * Handle the results from the voice data check activity.
     * Handle the results from a finished Alarm activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_ALARM_ACTIVITY){
            if(resultCode == AlarmActivity.RES_SAVED) {
                long alarmId = data.getLongExtra(AlarmActivity.KEY_ALARM_ID,-1);
                AlarmFunctions.engageAlarm(mDaoSession.getAlarmDao().loadByRowId(alarmId),this,mDaoSession,mAlarmManager);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Adds a new Alarm to the database and opens it in AlarmActivity
     * @param view
     */
    public void addAlarm(@Nullable View view) {
        startActivityForResult(AlarmActivity.newIntent(this, -1L), REQ_ALARM_ACTIVITY);
    }

    public void test(@Nullable View view) {
        List<Alarm> alarms = mDaoSession.getAlarmDao().loadAll();
        Alarm firstAlarm = alarms.get(0);
        DateTime now = new DateTime();
        now = now.withDurationAdded(new Duration(60 * 1000), 1);
        Set<String> blankRepeats = new HashSet<>();
        firstAlarm.setSchedule(now.getHourOfDay(),now.getMinuteOfHour(),blankRepeats);
        firstAlarm.setRepeats(1);
        firstAlarm.setActive(true);
        mDaoSession.getAlarmDao().update(firstAlarm);
        AlarmFunctions.engageAlarm(firstAlarm,this,mDaoSession,mAlarmManager);
        alarmListAdapter.updateAlarms();
    }



    /**
     * Enables the boot receiver, which restores the AlarmManager alarms
     * Needs to be called whenever there are active alarms to ensure they aren't lost on a reboot
     */
    public void enableBootReceiver(){
        ComponentName receiver = new ComponentName(this, BootBroadcastReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
    /**
     * Disables the boot receiver
     * Can be called whenever there are NO active alarms as it is not needed
     */
    public void disableBootReceiver(){
        ComponentName receiver = new ComponentName(this, BootBroadcastReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
