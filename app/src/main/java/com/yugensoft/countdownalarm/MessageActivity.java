package com.yugensoft.countdownalarm;

import android.content.Intent;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageActivity extends AppCompatActivity {

    private static final int MY_DATA_CHECK_CODE = 0;
    public TextToSpeech tts;

    private static final String TAG = "message-activity";

    private EditText editMessage;

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

    private InputFilter filter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            String blockCharacterSet = "{}";
            if (source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        editMessage = (EditText) findViewById(R.id.edit_message);
        editMessage.addTextChangedListener(messageTextWatcher);
        editMessage.setFilters(new InputFilter[] {filter});

        // get message 1 from db if exist, and load
        DaoSession daoSession = ((CountdownAlarmApplication)getApplication()).getDaoSession();
        MessageDao messageDao = daoSession.getMessageDao();

        if(messageDao.count()==0) {
            // default message
//            SpannableString msg1 = new SpannableString("Time to wake up! Today is ");
//            SpannableString msg2 = new SpannableString(" and the date is ");
//            DateTime now = DateTime.now();
//            SpannableString msgDay = new SpannableString(now.dayOfWeek().getAsText(Locale.getDefault()));
//            msgDay.setSpan(new TagSpan(Color.RED, Color.GREEN, 1), 0, msgDay.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            SpannableString msgDate = new SpannableString(now.toString("dd-MM-YY"));
//            msgDate.setSpan(new TagSpan(Color.BLUE, Color.WHITE, 2), 0, msgDate.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//            editMessage.setText(TextUtils.concat(msg1, msgDay, msg2, msgDate));

            editMessage.setText("Test message.");
        } else {
            List<Message> messages = messageDao.queryBuilder()
                    .where(MessageDao.Properties.Id.eq(1))
                    .list();
            Message message = messages.get(0);
            SpannableStringBuilder messageText = renderTaggedText(message.getText(),daoSession.getTagDao());

            editMessage.setText(messageText);
        }

        // check for TTS data
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);


    }

    public SpannableStringBuilder renderTaggedText(String text, TagDao tagDao) {
        StringBuffer sb = new StringBuffer();
        SpannableStringBuilder spannable = new SpannableStringBuilder();
        String regex = "\\{.*?\\}";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            sb.setLength(0); // clear
            String group = matcher.group();
            // caution, this code assumes your regex has single char delimiters
            String spanText = group.substring(1, group.length() - 1);

            // get the associated tag
            int tagId=Integer.valueOf(spanText);
            List<Tag> tagList = tagDao.queryBuilder().where(TagDao.Properties.Id.eq(tagId)).list();
            Tag tag;
            if(tagList.isEmpty()){
                // non-existent tag requested, ignore
                // todo: test
                continue;
            } else {
                tag = tagList.get(0);
            }
            // render it as the replacement text
            spanText = TagInserterFragment.renderTag(tag.getTagType(),tag.getSpeechFormat());

            matcher.appendReplacement(sb, spanText);
            spannable.append(sb.toString());
            int start = spannable.length() - spanText.length();

            spannable.setSpan(new TagSpan(Color.RED, Color.GREEN, tag), start, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        sb.setLength(0);
        matcher.appendTail(sb);
        spannable.append(sb.toString());
        return spannable;
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
        // detect any tags
        Editable text = editMessage.getText();
        TagSpan[] spans = text.getSpans(0,editMessage.length(),TagSpan.class);
        for (TagSpan span : spans){
            int spanEnd = text.getSpanEnd(span);
            int spanStart = text.getSpanStart(span);
            Log.d(TAG, "span:id="+span.getTag().getId()+","+span.getDrawnText()+ "," + String.valueOf(spanStart) + "," + String.valueOf(spanEnd));
        }

        tts.speak(editMessage.getText().toString(),TextToSpeech.QUEUE_FLUSH,null);
    }

    public void cancelMessage(View view) {
        finish();
    }

    public void saveMessage(View view) {
        // process the message and tags
        Spannable sMessage = editMessage.getText();
        String outputMessageText = "";
        TagSpan[] tagSpans = sMessage.getSpans(0,sMessage.length(),TagSpan.class);

        int previousEnd = 0;
        for (TagSpan ts : tagSpans){
            Tag t = ts.getTag();
            int start = sMessage.getSpanStart(ts);
            int end = sMessage.getSpanEnd(ts);
            // get previous un-spanned text
            outputMessageText += sMessage.subSequence(previousEnd,start);
            // convert this span to a text-representation of the tag id
            outputMessageText += "{" + t.getId().toString() + "}";
            previousEnd = end;
        }
        outputMessageText += sMessage.subSequence(previousEnd,sMessage.length());

        // replace in db
        DaoSession daoSession = ((CountdownAlarmApplication)getApplication()).getDaoSession();
        MessageDao messageDao = daoSession.getMessageDao();
        Message message = new Message(1L,outputMessageText);
        messageDao.insertOrReplace(message);


        finish();
    }

    public void insertTagDate(View view) {
        int cursorPos = editMessage.getSelectionStart();
        DateTagInserterFragment.newInstance(cursorPos,1L).show(getSupportFragmentManager(),"date-tag-inserter");
    }
}
