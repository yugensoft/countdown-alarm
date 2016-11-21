package com.yugensoft.countdownalarm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;


public class AlarmPlayerIntentService extends IntentService {
    public static final String KEY_RINGTONE_URI = "ringtone-uri";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_VIBRATE = "vibrate";

    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private TextToSpeech mTts;
    private boolean isSpeaking = false;
    private Vibrator mVibrator;
    private static final long[] VIBRATION_PATTERN = {0,500,500};

    // time settings (all in milliseconds)
    private static final long SETTING_MESSAGE_START_DELAY = 1000;
    private static final long SETTING_FADE_OUT_TO_TTS_TIME = 2000;
    private static final long SETTING_FADE_IN_TO_TTS_TIME = 2000;
    private static final long SETTING_REPEAT_DELAY = 5000;

    private String mRingtoneUri;
    private String mMessage;
    private boolean mVibrate;

    public static Intent newIntent(
            Context context,
            String ringtoneUri,
            boolean vibrate,
            @Nullable String message
    ){
        Intent intent = new Intent(context, AlarmPlayerIntentService.class);
        intent.putExtra(KEY_RINGTONE_URI, ringtoneUri);
        intent.putExtra(KEY_MESSAGE, message);
        intent.putExtra(KEY_VIBRATE, vibrate);
        return intent;
    }

    public AlarmPlayerIntentService() {
        super("AlarmPlayerIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mMediaPlayer = new MediaPlayer();
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // check for successful instantiation
                if (status == TextToSpeech.SUCCESS) {
                    if (mTts.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE) // todo: always 'US'?
                        mTts.setLanguage(Locale.US);
                } else if (status == TextToSpeech.ERROR) {
                    throw new RuntimeException("Text-to-speech failed.");
                }
            }
        });
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"stringId");

        TextToSpeech.OnUtteranceCompletedListener ttsListener = new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                fade(mMediaPlayer, SETTING_FADE_IN_TO_TTS_TIME, 0.2f, 1.0f);
                isSpeaking = false;
            }
        };

        try {
            // Start vibration
            if(mVibrate) {
                mVibrator.vibrate(VIBRATION_PATTERN, 0);
            }
            // Start alarm ringtone
            startAlarmSound(this, Uri.parse(mRingtoneUri));

            // Set initial volume
            mMediaPlayer.setVolume(1.0f,1.0f);

            Thread.sleep(SETTING_MESSAGE_START_DELAY);

            // Loop until intentService stopped
            while(true) {
                if(mMessage != null){
                    // Fade ready for message
                    fade(mMediaPlayer,SETTING_FADE_OUT_TO_TTS_TIME,1.0f,0.2f);

                    int test = 0;

                    // Play message
                    mTts.setOnUtteranceCompletedListener(ttsListener);
                    isSpeaking = true;
                    mTts.speak(mMessage, TextToSpeech.QUEUE_FLUSH, params);

                    while(isSpeaking){
                        Thread.sleep(200);
                    }
                }

                if (SETTING_REPEAT_DELAY < 0){
                    // no repeats
                    while (true) {
                        Thread.sleep(10000);
                    }
                }
                Thread.sleep(SETTING_REPEAT_DELAY);
            }

        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        } catch (IllegalStateException e) {
            // problem changing volume, probably because mediaPlayer killed for some reason
            // just let the service end
        }
    }

    /**
     * Fade a mediaPlayers volume in or out
     * @param mediaPlayer
     * @param fadeWindow
     * @param initialVolume
     * @param finalVolume
     * @return True if volume changed; false if error attempting to do so
     */
    private boolean fade(MediaPlayer mediaPlayer, long fadeWindow, float initialVolume, float finalVolume){
        final long TIME_INCREMENT = 100;
        float volume = initialVolume;

        try {

            // avoid div-by-zero
            if (fadeWindow == 0) {
                mediaPlayer.setVolume(finalVolume, finalVolume);
                return true;
            }

            for (long t = 0; t <= fadeWindow; t += TIME_INCREMENT) {
                try {
                    Thread.sleep(TIME_INCREMENT);
                } catch (InterruptedException e) {
                    // Restore interrupt status.
                    Thread.currentThread().interrupt();
                }
                volume = initialVolume + (finalVolume - initialVolume) * ((float) t / fadeWindow);
                mediaPlayer.setVolume(volume, volume);
            }
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void startAlarmSound(Context context, Uri alarm) {
        try {
            mMediaPlayer.setDataSource(context, alarm);
            if (mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            System.out.println("Problem playing the alarm");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRingtoneUri=intent.getStringExtra(KEY_RINGTONE_URI);
        mMessage=intent.getStringExtra(KEY_MESSAGE);
        mVibrate = intent.getBooleanExtra(KEY_VIBRATE, false);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mMediaPlayer.pause(); // stops sound faster than stop()
        mMediaPlayer.stop();
        mMediaPlayer.release();

        mTts.stop();
        mTts.shutdown();

        mVibrator.cancel();

        super.onDestroy();
    }
}
