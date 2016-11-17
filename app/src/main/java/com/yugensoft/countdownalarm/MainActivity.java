package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.analytics.Tracker;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static int REQ_ALARM_ACTIVITY = 0;

    private Tracker mTracker;
    private DaoSession mDaoSession;
    private AlarmManager mAlarmManager;

    private AlarmListAdapter alarmListAdapter;
    private ListView alarmListView;

    private Date mNextAlarm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        alarmListAdapter.updateAlarms();

        mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
//        engageAllAlarms(); //todo
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivityForResult(AlarmActivity.newIntent(this,id),REQ_ALARM_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_ALARM_ACTIVITY){
            if(resultCode == AlarmActivity.RES_SAVED) {
                alarmListAdapter.updateAlarms();
                long alarmId = data.getLongExtra(AlarmActivity.KEY_ALARM_ID,-1);
                engageAlarm(mDaoSession.getAlarmDao().loadByRowId(alarmId));
            }
        }
    }

    /**
     * Adds a new Alarm to the database and opens it in AlarmActivity
     * @param view
     */
    public void addAlarm(@Nullable View view) {
        Alarm alarm = Alarm.newDefaultAlarm();
        Long alarmId = mDaoSession.getAlarmDao().insertOrReplace(alarm);
        startActivityForResult(AlarmActivity.newIntent(this, alarmId), REQ_ALARM_ACTIVITY);
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
        engageAlarm(firstAlarm);
        alarmListAdapter.updateAlarms();
    }

    /**
     * Engage all active alarms, i.e. all active alarms get loaded into the AlarmManager ready to trigger
     * Does not disengage inactive alarms.
     * Assumes it is being called on application start or by the Boot Receiver
     */
    public void engageAllAlarms(){
        List<Alarm> alarms = mDaoSession.getAlarmDao().queryBuilder()
                .where(AlarmDao.Properties.Active.eq(true))
                .list();
        for (Alarm alarm : alarms){
            engageAlarm(alarm);
        }
    }

    /**
     * Engage/disengage the given alarm based on active status
     */
    public void engageAlarm(Alarm alarm){
        if(alarm.getId() == null){
            throw new IllegalArgumentException("Attempt to engage invalid alarm");
        }
        if(alarm.getId() > Integer.MAX_VALUE){
            throw new RuntimeException("Design failure, alarm ID exceeded integer maximum");
        }

        String message;
        if(alarm.getMessageId() == null) {
            message = null;
        } else {
            message = MessageActivity.renderTaggedText(alarm.getMessage().getText(),mDaoSession.getTagDao(),this).toString();
        }
        PendingIntent alarmIntent = PendingIntent.getActivity(
                this,
                alarm.getId().intValue(),
                AlarmReceiverActivity.newIntent(
                        this,
                        alarm.getId(),
                        alarm.getRingtone(),
                        alarm.getScheduleAlarmTime(this).humanReadable,
                        alarm.getVibrate(),
                        message
                ),
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        if(alarm.getActive()) {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, alarm.getNextAlarmTime().getTime(), alarmIntent);
        } else {
            mAlarmManager.cancel(alarmIntent);
        }
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
}
