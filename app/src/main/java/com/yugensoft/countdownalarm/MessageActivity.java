package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageActivity extends AppCompatActivity {

    private static final int MY_DATA_CHECK_CODE = 0;
    public TextToSpeech tts;

    private static final String TAG = "message-activity";

    //todo
    private TextWatcher messageTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    // setup TTS
    public void onInit(int initStatus) {

        // check for successful instantiation
        if (initStatus == TextToSpeech.SUCCESS) {
            if (tts.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
                tts.setLanguage(Locale.US);
        } else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        EditText message = (EditText) findViewById(R.id.edit_message);
        message.addTextChangedListener(messageTextWatcher);

        // default message
        SpannableString msg1 = new SpannableString("Time to wake up! Today is ");
        SpannableString msg2 = new SpannableString(" and the date is ");
        DateTime now = DateTime.now();
        SpannableString msgDay = new SpannableString(now.dayOfWeek().getAsText(Locale.getDefault()));
        msgDay.setSpan(new RoundedBackgroundSpan(Color.RED, Color.GREEN), 0, msgDay.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString msgDate = new SpannableString(now.toString("dd-MM-YY"));
        msgDate.setSpan(new RoundedBackgroundSpan(Color.BLUE, Color.WHITE), 0, msgDate.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        message.setText(TextUtils.concat(msg1,msgDay,msg2,msgDate));

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        // check for TTS data
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);


    }

    /**
     * Handle the results from the recognition activity.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // the user has the necessary data - create the TTS
                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {

                    }
                });
            } else {
                // no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent
                        .setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    public void previewMessage(View view) {
        EditText message = (EditText) findViewById(R.id.edit_message);

        // detect any tags
        Editable text = message.getText();
        RoundedBackgroundSpan[] spans = text.getSpans(0,message.length(),RoundedBackgroundSpan.class);
        for (RoundedBackgroundSpan span : spans){
            int spanEnd = text.getSpanEnd(span);
            int spanStart = text.getSpanStart(span);
            Log.d(TAG, "span:"+span.getDrawnText()+ "," + String.valueOf(spanStart) + "," + String.valueOf(spanEnd));
        }

        tts.speak(message.getText().toString(),TextToSpeech.QUEUE_FLUSH,null);
    }
}
