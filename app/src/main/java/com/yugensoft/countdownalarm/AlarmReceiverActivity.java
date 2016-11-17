package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.io.IOException;

public class AlarmReceiverActivity extends AppCompatActivity {
    public static final String KEY_ALARM_ID = "alarm-id";
    public static final String KEY_RINGTONE_URI = "ringtone-uri";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_ALARM_TIME = "alarm-time";
    public static final String KEY_VIBRATE = "vibrate";

    private TextView mTextTime;

    private long mAlarmId;
    private String mRingtoneUri;
    private String mAlarmTime;
    private String mMessage;
    private boolean mVibrate;

    private Intent mAlarmPlayerIntent;

    public static Intent newIntent(
            Context context,
            long alarmId,
            String ringtoneUri,
            String alarmTime,
            boolean vibrate,
            @Nullable String message
    ){
        Intent intent = new Intent(context, AlarmReceiverActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_ALARM_ID, alarmId);
        intent.putExtra(KEY_RINGTONE_URI, ringtoneUri);
        intent.putExtra(KEY_MESSAGE, message);
        intent.putExtra(KEY_ALARM_TIME, alarmTime);
        intent.putExtra(KEY_VIBRATE, vibrate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_alarm_receiver);

        mTextTime = (TextView)findViewById(R.id.text_time);

        // load arguments
        Intent intent = getIntent();
        mAlarmId = intent.getLongExtra(KEY_ALARM_ID,-1);
        mAlarmTime = intent.getStringExtra(KEY_ALARM_TIME);
        mMessage = intent.getStringExtra(KEY_MESSAGE);
        mRingtoneUri = intent.getStringExtra(KEY_RINGTONE_URI);
        mVibrate = intent.getBooleanExtra(KEY_VIBRATE, false);

        DateTime now = new DateTime();
        String time = AlarmTimeFormatter.convertTimeToReadable(now.getHourOfDay(),now.getMinuteOfHour(),this);
        mTextTime.setText(time);

        // Start the alarm player
        mAlarmPlayerIntent = AlarmPlayerIntentService.newIntent(this,mRingtoneUri,mVibrate,mMessage);
        startService(mAlarmPlayerIntent);
    }

    public void snoozeAlarm(@Nullable View view) {
        // todo: set off a new pendingintent back to this activity
    }

    public void dismissAlarm(@Nullable View view) {
        stopService(mAlarmPlayerIntent);
        finish();
    }
}
