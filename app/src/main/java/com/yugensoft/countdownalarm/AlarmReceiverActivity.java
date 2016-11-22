package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.Date;

public class AlarmReceiverActivity extends AppCompatActivity {
    public static final String KEY_ALARM_ID = "alarm-id";
    public static final String KEY_RINGTONE_URI = "ringtone-uri";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_ALARM_TIME = "alarm-time";
    public static final String KEY_VIBRATE = "vibrate";
    public static final String KEY_TRIGGERED_TIME = "trig-time";
    public static final String KEY_SNOOZE_DURATION = "snooze-dur";
    public static final String KEY_PREVIEW = "preview";

    private TextView mTextTime;

    // loaded from intent
    private long mAlarmId;
    private String mRingtoneUri;
    private String mAlarmTime;
    private String mMessage;
    private boolean mVibrate;
    private long mTriggeredTime;
    private long mSnoozeDuration;
    private boolean mIsPreview;

    private Intent mAlarmPlayerIntent;

    private AlarmManager mAlarmManager;

    private DaoSession mDaoSession;

    public static Intent newIntent(
            Context context,
            long alarmId,
            String ringtoneUri,
            String alarmTime,
            long triggeredTime,
            long snoozeDuration,
            boolean vibrate,
            boolean isPreview,
            @Nullable String message
    ){
        Intent intent = new Intent(context, AlarmReceiverActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_ALARM_ID, alarmId);
        intent.putExtra(KEY_RINGTONE_URI, ringtoneUri);
        intent.putExtra(KEY_MESSAGE, message);
        intent.putExtra(KEY_ALARM_TIME, alarmTime);
        intent.putExtra(KEY_VIBRATE, vibrate);
        intent.putExtra(KEY_TRIGGERED_TIME, triggeredTime);
        intent.putExtra(KEY_SNOOZE_DURATION,snoozeDuration);
        intent.putExtra(KEY_PREVIEW,isPreview);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_alarm_receiver);

        mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        mTextTime = (TextView)findViewById(R.id.text_time);

        // load arguments
        Intent intent = getIntent();
        mAlarmId = intent.getLongExtra(KEY_ALARM_ID,-1);
        mAlarmTime = intent.getStringExtra(KEY_ALARM_TIME);
        mMessage = intent.getStringExtra(KEY_MESSAGE);
        mRingtoneUri = intent.getStringExtra(KEY_RINGTONE_URI);
        mVibrate = intent.getBooleanExtra(KEY_VIBRATE, false);
        mTriggeredTime = intent.getLongExtra(KEY_TRIGGERED_TIME,-1);
        mSnoozeDuration = intent.getLongExtra(KEY_SNOOZE_DURATION,-1);
        mIsPreview = intent.getBooleanExtra(KEY_PREVIEW,false);

        // show the time
        DateTime now = new DateTime();
        String time = AlarmTimeFormatter.convertTimeToReadable(now.getHourOfDay(),now.getMinuteOfHour(),this);
        mTextTime.setText(time);

        // Start the alarm player
        mAlarmPlayerIntent = AlarmPlayerIntentService.newIntent(this,mRingtoneUri,mVibrate,mMessage);
        startService(mAlarmPlayerIntent);

        // get the next alarm time and set it
        if(!mIsPreview) {
            mDaoSession = ((CountdownAlarmApplication) getApplication()).getDaoSession();
            Alarm alarm = mDaoSession.getAlarmDao().loadByRowId(mAlarmId);
            Integer repeats = alarm.getRepeats();
            if (repeats == null) {
                // ongoing repeating alarm, just re-engage it
                AlarmFunctions.engageAlarm(alarm, this, mDaoSession, mAlarmManager);

            } else if (repeats > 1) { // stub: fixed-repeats alarms not yet implemented
                // fixed repeats alarm, re-engage it and update it in db
                repeats--;
                alarm.setRepeats(repeats);
                alarm.setActive(true); // should be anyway, but affirm
                AlarmFunctions.engageAlarm(alarm, this, mDaoSession, mAlarmManager);
                alarm.update();

            } else if (repeats == 1) {
                // no-repeat alarm, deactivate and disengage it
                alarm.setRepeats(0);
                alarm.setActive(false);
                AlarmFunctions.engageAlarm(alarm, this, mDaoSession, mAlarmManager);
                alarm.update();
            }
        }
    }

    /**
     * Set of a new alarm pendingIntent back to this activity with the snooze delay added
     * Then dismiss.
     * @param view
     */
    public void snoozeAlarm(@Nullable View view) {
        long snoozedAlarmTime = mTriggeredTime + mSnoozeDuration;

        Intent intent = AlarmReceiverActivity.newIntent(
                this,
                mAlarmId,
                mRingtoneUri,
                mAlarmTime,
                snoozedAlarmTime,
                mSnoozeDuration,
                mVibrate,
                mIsPreview,
                mMessage
        );

        PendingIntent alarmIntent = PendingIntent.getActivity(
                this,
                Long.valueOf(mAlarmId).intValue(),
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        mAlarmManager.set(AlarmManager.RTC_WAKEUP, snoozedAlarmTime, alarmIntent);

        Toast.makeText(this, "Snoozing...", Toast.LENGTH_SHORT).show();

        dismissAlarm(null);
    }

    public void dismissAlarm(@Nullable View view) {
        stopService(mAlarmPlayerIntent);
        finish();
    }
}
