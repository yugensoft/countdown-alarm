package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.Intent;
import android.os.LocaleList;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
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

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageActivity extends AppCompatActivity {

    public TextToSpeech tts;

    private static final String TAG = "message-activity";

    public static final String KEY_MESSAGE_ID = "message-id";
    public static final String KEY_MAKE_NEW_MESSAGE = "make-message";
    public static final int RES_CANCELED = 0;
    public static final int RES_SAVED = 1;
    public static final String RES_MESSAGE_TEXT = "message-text";
    public static final String RES_MESSAGE_ID = "message-id";

    private EditText editMessage;

    // loaded from intent
    private Long mMessageId;

    // keeps track of if Tts is read to use
    private boolean mTtsReady = true;

    private Tracker mTracker;

    /**
     * Get a new intent for starting this activity
     * @param context
     * @param messageId
     * @return
     */
    public static Intent newIntent(Context context, @Nullable Long messageId){
        Intent intent = new Intent(context, MessageActivity.class);
        if(messageId == null){
            intent.putExtra(KEY_MAKE_NEW_MESSAGE,true);
        } else {
            intent.putExtra(KEY_MESSAGE_ID, messageId);
        }
        return intent;
    }

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

        // create the tts
        tts = TextToSpeechFactory.getTts(this, new TextToSpeechFactory.TextToSpeechDataDownloadCallback() {
            @Override
            public boolean onTtsDataDownloadStart() {
                mTtsReady = false;
                Toast.makeText(MessageActivity.this, R.string.tts_data_missing_message, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Set up the message edittext
        editMessage = (EditText) findViewById(R.id.edit_message);
        editMessage.addTextChangedListener(messageTextWatcher);
        editMessage.setFilters(new InputFilter[] {filter});

        // get message from db
        Message message;
        DaoSession daoSession = ((CountdownAlarmApplication)getApplication()).getDaoSession();
        MessageDao messageDao = daoSession.getMessageDao();
        String taggedText;
        if(getIntent().getExtras().getBoolean(KEY_MAKE_NEW_MESSAGE)){
            taggedText = getString(R.string.default_message);
            mMessageId = null;
        } else {
            mMessageId = getIntent().getExtras().getLong(KEY_MESSAGE_ID);
            List<Message> messages = messageDao.queryBuilder()
                    .where(MessageDao.Properties.Id.eq(mMessageId))
                    .list();
            if(messages.size() != 1){
                throw new RuntimeException("Invalid ID passed or more than one message with that ID");
            }
            message = messages.get(0);
            taggedText = message.getText();
        }

        // populate edittext with message
        SpannableStringBuilder messageText = renderTaggedText(taggedText, daoSession.getTagDao(), this);
        editMessage.setText(messageText);

        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication)getApplication()).getDefaultTracker();
    }

    /**
     * Handle the result of any TTS data downloads, if it happens
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == TextToSpeechFactory.REQ_MISSING_DATA_DOWNLOAD){
            // try to start the TTS again
            tts = TextToSpeechFactory.getTts(this, new TextToSpeechFactory.TextToSpeechDataDownloadCallback() {
                @Override
                public boolean onTtsDataDownloadStart() {
                    throw new RuntimeException("TTS data is missing and its download wasn't requested.");
                }
            });
            mTtsReady = true;
        }
    }

    public static SpannableStringBuilder renderTaggedText(String text, TagDao tagDao, Context context) {
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
            spanText = TagInserterFragment.renderTag(
                    context.getResources(),
                    tag.getTagType(),
                    tag.getSpeechFormat(),
                    tag.getCompareDate(),
                    null
            );

            matcher.appendReplacement(sb, spanText);
            spannable.append(sb.toString());
            int start = spannable.length() - spanText.length();

            spannable.setSpan(new TagSpan(tag, context), start, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        sb.setLength(0);
        matcher.appendTail(sb);
        spannable.append(sb.toString());
        return spannable;
    }

    public void previewMessage(View view) {
        // check tts ready
        if(!mTtsReady){
            Toast.makeText(this, R.string.tts_download_in_prog, Toast.LENGTH_SHORT).show();
            return;
        }

        // detect any tags
        Editable text = editMessage.getText();
        TagSpan[] spans = text.getSpans(0,editMessage.length(),TagSpan.class);
        for (TagSpan span : spans){
            int spanEnd = text.getSpanEnd(span);
            int spanStart = text.getSpanStart(span);
        }

        tts.speak(editMessage.getText().toString(),TextToSpeech.QUEUE_FLUSH,null);

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("previewMessage")
                .build());
    }

    public void cancelMessage(View view) {
        setResult(RES_CANCELED);
        finish();
    }

    /**
     * Save the message into the database and return it's raw text to calling activity
     * @param view
     */
    public void saveMessage(@Nullable View view) {
        // process the message and tags
        Spannable sMessage = editMessage.getText();
        String outputMessageText = "";
        TagSpan[] tagSpans = sMessage.getSpans(0,sMessage.length(),TagSpan.class);

        int previousEnd = 0;
        ArrayList<Long> listOfUsedIds = new ArrayList<>(); // for use in where string, all other tags should be discarded
        for (TagSpan ts : tagSpans){
            Tag t = ts.getTag();
            listOfUsedIds.add(t.getId());

            int start = sMessage.getSpanStart(ts);
            int end = sMessage.getSpanEnd(ts);
            // get previous un-spanned text
            outputMessageText += sMessage.subSequence(previousEnd,start);
            // convert this span to a text-representation of the tag id
            outputMessageText += "{" + t.getId().toString() + "}";
            previousEnd = end;
        }
        outputMessageText += sMessage.subSequence(previousEnd,sMessage.length());

        // replace in db, or delete if made blank
        DaoSession daoSession = ((CountdownAlarmApplication) getApplication()).getDaoSession();
        MessageDao messageDao = daoSession.getMessageDao();
        String returnString;
        Message message = null;
        if(outputMessageText.trim().length() == 0) {
            if(mMessageId != null) {
                // delete the message
                messageDao.deleteByKey(mMessageId);
                // remove unused tags from db
                TagDao tagDao = daoSession.getTagDao();
                tagDao.queryBuilder()
                        .where(TagDao.Properties.MessageId.eq(mMessageId))
                        .buildDelete()
                        .executeDeleteWithoutDetachingEntities();
            }

            returnString = null;
        } else {
            message = new Message(mMessageId, outputMessageText);
            mMessageId = messageDao.insertOrReplace(message);

            // remove unused tags from db
            TagDao tagDao = daoSession.getTagDao();
            tagDao.queryBuilder()
                    .where(TagDao.Properties.Id.notIn(listOfUsedIds))
                    .where(TagDao.Properties.MessageId.eq(mMessageId))
                    .buildDelete()
                    .executeDeleteWithoutDetachingEntities();

            returnString = sMessage.toString().trim();
        }

        // Tell the calling activity
        Intent intent = new Intent();
        intent.putExtra(RES_MESSAGE_TEXT, returnString);
        intent.putExtra(RES_MESSAGE_ID, mMessageId);
        setResult(RES_SAVED,intent);
        finish();
    }

    public void insertTagDate(View view) {
        int cursorPos = editMessage.getSelectionStart();
        DateTagInserterFragment.newInstance(cursorPos,1L).show(getSupportFragmentManager(),"date-tag-inserter");

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("insertTagDate")
                .build());
    }

    public void insertTagCountdown(View view) {
        int cursorPos = editMessage.getSelectionStart();
        CountingTagInserterFragment.newInstance(cursorPos,1L, CountingTagInserterFragment.CountingDirection.DOWN)
                .show(getSupportFragmentManager(),"countdown-tag-inserter");

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("insertTagCountdown")
                .build());
    }

    public void insertTagCountup(View view) {
        int cursorPos = editMessage.getSelectionStart();
        CountingTagInserterFragment.newInstance(cursorPos,1L, CountingTagInserterFragment.CountingDirection.UP)
                .show(getSupportFragmentManager(),"countup-tag-inserter");

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("insertTagCountup")
                .build());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
