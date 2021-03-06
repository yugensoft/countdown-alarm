package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "app-debug";
    private static final boolean DEBUG_LICENSE_AGREE = false;
    private static final boolean DEBUG_EXAMPLE_ALARM = false;

    private static final int EXAMPLE_ALARM_HOUR = 8;
    private static final int EXAMPLE_ALARM_MINUTE = 30;

    private static int REQ_ALARM_ACTIVITY = 0;

    public static final String KEY_HAS_AGREED = "agreed";
    private static final String KEY_HAS_FIRST_RUN = "firstrun";

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
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_HAS_AGREED, false)) {
            startActivity(new Intent(this, LicenseAgreementActivity.class));
            finish();
        }
        if (DEBUG_LICENSE_AGREE) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MainActivity.KEY_HAS_AGREED, false).apply();
        }

        // Get the shared tracker instance
        mTracker = ((CountdownAlarmApplication) getApplication()).getDefaultTracker();
        // get Dao
        mDaoSession = ((CountdownAlarmApplication) getApplication()).getDaoSession();
        // get views
        alarmListView = (ListView) findViewById(R.id.listview_alarms);

        // Check if this is the first run, and if so add the example alarm
        if(DEBUG_EXAMPLE_ALARM){
            PreferenceManager.getDefaultSharedPreferences(this).edit().remove(MainActivity.KEY_HAS_FIRST_RUN).apply();
            mDaoSession.getAlarmDao().deleteAll();
            mDaoSession.getTagDao().deleteAll();
            mDaoSession.getMessageDao().deleteAll();
        }
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_HAS_FIRST_RUN, false)) {
            insertExampleAlarm();

            // save
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MainActivity.KEY_HAS_FIRST_RUN, true).apply();
        }

        // Alarm list
        alarmListAdapter = new AlarmListAdapter(
                this,
                mDaoSession
        );
        alarmListView.setAdapter(alarmListAdapter);
        alarmListView.setOnItemClickListener(this);

        // engage all alarms
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmFunctions.engageAllAlarms(this, mDaoSession, mAlarmManager);

        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication) getApplication()).getDefaultTracker();

        // The ad
        mAdView = (AdView) findViewById(R.id.adView);
        MiscFunctions.loadAdIntoAdView(mAdView);

    }

    /**
     * Inserts an example alarm to provide a better user landing experience
     * The example alarm has a message, and that message has a tag in it.
     */
    private void insertExampleAlarm() {
        MessageDao messageDao = mDaoSession.getMessageDao();
        AlarmDao alarmDao = mDaoSession.getAlarmDao();
        TagDao tagDao = mDaoSession.getTagDao();

        // make message with placeholder text
        Message message = new Message();
        message.setText("---");
        long messageId = messageDao.insert(message);

        // create a date tag
        Tag tag = new Tag();
        tag.setMessageId(messageId);
        tag.setTagType(Tag.TagType.TODAYS_DATE);
        tag.setSpeechFormat(getString(R.string.speech_format_day_only));
        long tagId = tagDao.insert(tag);

        // create the real message which includes the tag, and update it
        message.setText(getString(R.string.example_alarm, tagId));
        messageDao.update(message);

        // create the example alarm
        Alarm alarm = new Alarm();
        alarm.setActive(false);
        alarm.setMessage(message);
        alarm.setLabel(getString(R.string.my_example_alarm));
        alarm.setRingtone(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()); // use default
        alarm.setVibrate(false);
        alarm.setRepeats(null); // forever
        HashSet<String> allDays = new HashSet<String>(Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
        alarm.setSchedule(EXAMPLE_ALARM_HOUR, EXAMPLE_ALARM_MINUTE, allDays);
        alarmDao.insert(alarm);

//        return new Alarm(null, "0 0 8 ? * * *", 1, true, ringtone, true, "", null);

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
            case R.id.mi_about:
                new AboutFragment().show(getSupportFragmentManager(), "about_dialog");
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
