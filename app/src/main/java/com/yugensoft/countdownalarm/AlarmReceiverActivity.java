package com.yugensoft.countdownalarm;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AlarmReceiverActivity extends AppCompatActivity {
    public static final String KEY_ALARM_ID = "alarm-id";
    public static final String KEY_RINGTONE_URI = "ringtone-uri";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_ALARM_TIME = "alarm-time";

    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;
    private static final long[] VIBRATION_PATTERN = {0,500,500};

    private TextView mTextAlarmTime;

    private long mAlarmId;
    private String mRingtoneUri;
    private String mAlarmTime;
    private String mMessage;

    public static Intent newIntent(Context context, long alarmId, String ringtoneUri, String alarmTime, @Nullable String message){
        Intent intent = new Intent(context, AlarmReceiverActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_ALARM_ID, alarmId);
        intent.putExtra(KEY_RINGTONE_URI, ringtoneUri);
        intent.putExtra(KEY_MESSAGE, message);
        intent.putExtra(KEY_ALARM_TIME, alarmTime);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_alarm_receiver);

        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        mMediaPlayer = new MediaPlayer();

        mTextAlarmTime = (TextView)findViewById(R.id.text_alarm_time);

        // load arguments
        Intent intent = getIntent();
        mAlarmId = intent.getLongExtra(KEY_ALARM_ID,-1);
        mAlarmTime = intent.getStringExtra(KEY_ALARM_TIME);
        mMessage = intent.getStringExtra(KEY_MESSAGE);
        mRingtoneUri = intent.getStringExtra(KEY_RINGTONE_URI);

        mTextAlarmTime.setText(mAlarmTime); // todo: design decision: alarm time or current time?
        playSound(this, Uri.parse(mRingtoneUri));
        mVibrator.vibrate(VIBRATION_PATTERN,0);

    }

    private void playSound(Context context, Uri alarm) {
        try {
            mMediaPlayer.setDataSource(context, alarm);
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            System.out.println("Problem playing the alarm");
        }
    }

    public void snoozeAlarm(@Nullable View view) {
        // todo: set off a new pendingintent back to this activity
    }

    public void dismissAlarm(@Nullable View view) {
        mMediaPlayer.stop();
        mVibrator.cancel();
        finish();
    }
}
