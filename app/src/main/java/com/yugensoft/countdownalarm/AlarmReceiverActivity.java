package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;

import static com.yugensoft.countdownalarm.AlarmFunctions.getAllAlarmsSorted;
import static com.yugensoft.countdownalarm.AlarmFunctions.getNextAlarmTimeReadable;
import static com.yugensoft.countdownalarm.AlarmFunctions.setNotification;

public class AlarmReceiverActivity extends AppCompatActivity {
    private static final String TAG = "alarm-receiver";

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

    private boolean mUserDismissed;
    private boolean mUserSnoozed;
    private boolean mFocusDuringOnPause;
    private PowerManager mPowerManager;

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
        mDaoSession = ((CountdownAlarmApplication) getApplication()).getDaoSession();

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
        String time = TimeFormatters.convertTimeToReadable(now.getHourOfDay(),now.getMinuteOfHour(),this);
        mTextTime.setText(time);

        // get the next alarm time and set it
        if(!mIsPreview) {
            Alarm alarm = mDaoSession.getAlarmDao().loadByRowId(mAlarmId);
            Integer repeats = alarm.getRepeats();
            if (repeats == null) {
                // ongoing repeating alarm, just re-engage it
                AlarmFunctions.engageAlarm(alarm, this, mDaoSession, mAlarmManager);

            } else if (repeats > 1) {
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

        // Ensure screen unlocked and awake
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // Should only ever be dismissed if user presses the dismiss button, otherwise assume 'snooze'
        mUserDismissed = false;
        mUserSnoozed = false;
    }

    /**
     * Set off a new alarm pendingIntent back to this activity with the snooze delay added
     * Then finish this activity.
     * @param view
     */
    public void snoozeAlarm(@Nullable View view) {
        long snoozedAlarmTime = new Date().getTime() + mSnoozeDuration;

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

        Toast.makeText(this, R.string.snoozing, Toast.LENGTH_SHORT).show();

        mUserSnoozed = true;
        stopService(mAlarmPlayerIntent);
        mAlarmPlayerIntent = null;
        finish();
    }

    /**
     * Stop the alarm and finish this activity.
     * @param view Optional calling view.
     */
    public void dismissAlarm(@Nullable View view) {
        mUserDismissed = true;
        stopService(mAlarmPlayerIntent);
        mAlarmPlayerIntent = null;

        // update the system notification of next alarm
        updateNextAlarmSystemNotification();
    }

    /**
     * Update the system notification of next alarm
     * Then finish.
     */
    public void updateNextAlarmSystemNotification(){
        AlarmFunctions.GetAlarmsCallback callback = new AlarmFunctions.GetAlarmsCallback() {
            @Override
            public void callback(final List<Alarm> alarms) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update notification
                        String nextAlarmTime = getNextAlarmTimeReadable(alarms, AlarmReceiverActivity.this);
                        setNotification(nextAlarmTime,AlarmReceiverActivity.this,getString(R.string.app_name));

                        // finish the activity
                        finish();
                    }
                });
            }
        };

        getAllAlarmsSorted(mDaoSession,callback);

    }

    @Override
    protected void onStart() {
//        Log.d(TAG, "onStart: " + (mPowerManager.isScreenOn() ? "screen on" : "screen off"));

        // don't start the alarm if screen is still off, wait for next cycle
        if(mPowerManager.isScreenOn()){
            // Start the alarm player
            mAlarmPlayerIntent = AlarmPlayerIntentService.newIntent(this,mRingtoneUri,mVibrate,mMessage);

            startService(mAlarmPlayerIntent);

        }

        super.onStart();
    }

    /**
     * Desired behaviour:
     * pressed home: snooze
     * pressed back: snooze
     * rotated screen: nothing, maintain state
     * some other activity comes to front: pause alarm, resume it when resume
     * pressed snooze: snooze
     * pressed dismiss: dismiss
     */
    @Override
    protected void onStop() {
//        Log.d(
//                TAG, "onStop: " +
//                " by user button:"+String.valueOf(mUserDismissed||mUserSnoozed) +
//                " change config:"+String.valueOf(isChangingConfigurations()) +
//                " screen on:"+String.valueOf(mPowerManager.isScreenOn())
//        );

        if(!mUserDismissed && !mUserSnoozed){
            // Activity is stopping without users intent to do so, and without an orientation change
            // Treat this as a snooze trigger, to ensure alarm not lost
            if(!mFocusDuringOnPause){
                // a startup when the window didn't have focus, such as during lock
            } else if(!isChangingConfigurations()) {
                snoozeAlarm(null);
            }
        }
        // stop the alarm sound, if not already (by a button, for speed purposes)
        if(mAlarmPlayerIntent != null) {
            stopService(mAlarmPlayerIntent);
        }

        super.onStop();
    }

    public void onPause() {
//        Log.d(TAG, "onPause: " + (hasWindowFocus() ? "window focus" : "no window focus"));

        mFocusDuringOnPause = hasWindowFocus();
        super.onPause();
    }



}
