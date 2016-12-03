package com.yugensoft.countdownalarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;

import java.util.Locale;

/**
 * Factory class to do the work required to get a guaranteed working fully-ready-to-use TTS instance
 */
public class TextToSpeechFactory  {
    public static final int REQ_MISSING_DATA_DOWNLOAD = 60;


    private TextToSpeech tts;
    private Context mContext;
    private TextToSpeechDataDownloadCallback mCallback;

    /**
     * Factory method
     * @param context
     * @param callback The onTtsDataDownloadStart for attempted downloads
     * @return
     */
    public static TextToSpeech getTts(Context context, @Nullable TextToSpeechDataDownloadCallback callback){
        TextToSpeechFactory factory = new TextToSpeechFactory(context, callback);
        return factory.getTts();
    }

    private TextToSpeechFactory(Context context, @Nullable TextToSpeechDataDownloadCallback callback){
        // create the TTS
        mContext = context;
        mCallback = callback;
        tts = new TextToSpeech(context, initListener);
    }
    private TextToSpeech getTts(){
        return tts;
    }

    private TextToSpeech.OnInitListener initListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            // check for successful instantiation
            if (status == TextToSpeech.SUCCESS) {
                setTtsSettings();
            } else if (status == TextToSpeech.ERROR) {
                throw new RuntimeException("Text-to-speech failed.");
            }
        }
    };

    /**
     * Try to set the TTS language, and try language download if necessary
     */
    private void setTtsSettings() {
        // try local language
        switch (tts.isLanguageAvailable(Locale.getDefault())) {
            case TextToSpeech.LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                tts.setLanguage(Locale.getDefault());
                return;
            case TextToSpeech.LANG_MISSING_DATA:
                installMissingData();
                return;
            case TextToSpeech.LANG_NOT_SUPPORTED:
                // perhaps Locale default is too rare, continue and try US
                break;
            default:
                throw new RuntimeException("Unknown TTS language status");

        }
        // otherwise, try US
        switch (tts.isLanguageAvailable(Locale.US)) {
            case TextToSpeech.LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                tts.setLanguage(Locale.getDefault());
                break;
            case TextToSpeech.LANG_MISSING_DATA:
                installMissingData();
                break;
            case TextToSpeech.LANG_NOT_SUPPORTED:
                throw new RuntimeException("Couldn't setup TTS engine, neither default Locale nor US supported.");
            default:
                throw new RuntimeException("Unknown TTS language status");

        }

    }

    /**
     * Method called when TTS needs to install missing data
     */
    private void installMissingData() {
        if(mCallback != null){
            if(!mCallback.onTtsDataDownloadStart()) return;
        }

        Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if(mContext instanceof Activity){
            ((Activity)mContext).startActivityForResult(intent, REQ_MISSING_DATA_DOWNLOAD);
        }
    }

    /**
     * Callback when download is going to be attempted
     * Return false on onTtsDataDownloadStart to terminate download
     */
    public static abstract class TextToSpeechDataDownloadCallback {
        public abstract boolean onTtsDataDownloadStart();
    }
}
