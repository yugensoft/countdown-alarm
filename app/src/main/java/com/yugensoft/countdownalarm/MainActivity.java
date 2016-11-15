package com.yugensoft.countdownalarm;

import android.content.Intent;
import android.media.RingtoneManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.analytics.Tracker;

import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static int REQ_ALARM_ACTIVITY = 0;

    private Tracker mTracker;
    private DaoSession mDaoSession;
    private AlarmListAdapter alarmListAdapter;
    private ListView alarmListView;


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
        alarmListAdapter.updateAlarms();
        alarmListView.setAdapter(alarmListAdapter);
        alarmListView.setOnItemClickListener(this);

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
            }
        }
    }

    public void addTempAlarm(View view) {
        String ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
        Alarm alarm = new Alarm(1L,"0 30 8 ? * * *",1,true,ringtone,true,"test",null);
//        List<Message> messages = mDaoSession.getMessageDao().queryBuilder()
//                .where(MessageDao.Properties.Id.eq(1))
//                .list();
//        Message message = messages.get(0);
//        alarm.setMessage(message);
        mDaoSession.getAlarmDao().insertOrReplace(alarm);
        alarmListAdapter.updateAlarms();
    }
}
