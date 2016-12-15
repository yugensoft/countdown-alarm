package com.yugensoft.countdownalarm;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.analytics.Tracker;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;

public class TagInserterFragment extends DialogFragment {
    private static final String TAG = "tag-inserter";

    protected static final String KEY_CURSOR_POS = "cursor-pos";
    protected static final String KEY_MESSAGE_ID = "message-id";

    protected static final String COMPARE_DATE_STORAGE_FORMAT = "MM dd";

    private static final String KEY_COMPARE_DATE = "com-date";
    private static final String KEY_SPEECH_FORMAT = "speech-format";


    protected View mFragmentView;
    protected int mCursorPos;
    protected long mMessageId;

    // state data
    protected Tag.TagType mTagType; // always constructed by child onCreates() from the bundle
    protected String mCompareDate; // to save
    protected String mSpeechFormat; // to save

    protected Tracker mTracker;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication)getActivity().getApplication()).getDefaultTracker();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.insert_tag_title);


        return dialog;
    }

    /**
     * Render a tag.
     * @param resources Resource object, used to get string resources.
     * @param tagType The tag type, countdown/countup/date etc.
     * @param speechFormat Format for how to render dates into phrases.
     * @param referenceTime Reference time against which durations are calculated. Now if null.
     * @param comparisonDate Date to compare against to get a duration.
     * @param prependFormat Whether or not to prepend explanatory text.
     * @return Rendered tag string.
     */
    public static String renderTag(
            Resources resources,
            Tag.TagType tagType,
            String speechFormat,
            @Nullable Long referenceTime,
            @Nullable String comparisonDate,
            @Nullable Boolean prependFormat
    ) {
        String renderedOutput;
        DateTime now;
        if (referenceTime == null){
            now = DateTime.now();
        } else {
            now = new DateTime(referenceTime);
        }

        DateTime comDate,nowDate;
        Days daysBetween;
        switch(tagType){
            case COUNTDOWN:
                if(comparisonDate == null){
                    throw new RuntimeException("comparisonDate expected");
                }

                comDate = DateTime.parse("0000 " + comparisonDate, DateTimeFormat.forPattern("yyyy " + COMPARE_DATE_STORAGE_FORMAT));
                nowDate = new DateTime(0,now.getMonthOfYear(),now.getDayOfMonth(),0,0);
                daysBetween = Days.daysBetween(nowDate,comDate);
                if(daysBetween.getDays() < 0){
                    comDate = DateTime.parse("0001 " + comparisonDate, DateTimeFormat.forPattern("yyyy " + COMPARE_DATE_STORAGE_FORMAT));
                    daysBetween = Days.daysBetween(nowDate,comDate);
                }

                renderedOutput = TimeFormatters.convertDaysToSpeechFormat(resources, daysBetween,speechFormat,prependFormat);

                break;
            case COUNTUP:
                if(comparisonDate == null){
                    throw new RuntimeException("comparisonDate expected");
                }

                comDate = DateTime.parse("0000 " + comparisonDate, DateTimeFormat.forPattern("yyyy " + COMPARE_DATE_STORAGE_FORMAT));
                nowDate = new DateTime(1,now.getMonthOfYear(),now.getDayOfMonth(),0,0);
                daysBetween = Days.daysBetween(comDate,nowDate);
                if(daysBetween.getDays() > 365){
                    nowDate = new DateTime(0,now.getMonthOfYear(),now.getDayOfMonth(),0,0);
                    daysBetween = Days.daysBetween(comDate,nowDate);
                }

                renderedOutput = TimeFormatters.convertDaysToSpeechFormat(resources,daysBetween,speechFormat,prependFormat);

                break;
            case TODAYS_DATE:
                // Custom field 'dddd' is replaced manually with 1st,2nd,3rd etc
                String speechFormatExt = speechFormat.replace(
                        "dddd",
                        "'" + String.valueOf(now.getDayOfMonth()) + MiscFunctions.getLastDigitSufix(resources,now.getDayOfMonth()) + "'"
                );
                renderedOutput = now.toString(speechFormatExt);
                break;
            default:
                throw new RuntimeException("Unknown tag type");
        }

        return renderedOutput;
    }

    /**
     * Insert a tag into the EditText of the MessageActivity, using the data in this Fragment
     * @throws TagPropertyMissingException
     */
    protected void insertTag() throws TagPropertyMissingException {
        Tag tag = new Tag();
        tag.setMessageId(mMessageId);

        // check and set tag properties
        if(mTagType != null && mTagType != Tag.TagType.UNKNOWN) {
            tag.setTagType(mTagType);
        } else {
            throw new RuntimeException("TagType not set or unknown");
        }
        if(mCompareDate != null) {
            tag.setCompareDate(mCompareDate);
        } else {
            // only 'todays date' tag allows no compare date
            if(mTagType != Tag.TagType.TODAYS_DATE) {
                throw new TagPropertyMissingException(getString(R.string.choose_date));
            }
        }
        if(mSpeechFormat != null) {
            tag.setSpeechFormat(mSpeechFormat);
        } else {
            throw new TagPropertyMissingException(getString(R.string.choose_speech));
        }

        // insert into tags db table
        DaoSession daoSession = ((CountdownAlarmApplication)getActivity().getApplication()).getDaoSession();
        TagDao tagDao = daoSession.getTagDao();
        long tagId = tagDao.insert(tag);

        // generate textual representation of the tag
        String tagRendering = renderTag(getResources(),mTagType,mSpeechFormat,null,mCompareDate,null);

        /**
         * insert span into edittext
         */
        EditText editMessage = (EditText)getActivity().findViewById(R.id.edit_message);
        Spannable str = editMessage.getText();
        Spannable before = new SpannableStringBuilder(str,0,mCursorPos);
        Spannable after = new SpannableStringBuilder(str,mCursorPos,str.length());
        Spannable newSpan = new SpannableStringBuilder(tagRendering);
        // set colors
        newSpan.setSpan(
                new TagSpan(tag,getActivity()),
                0,
                newSpan.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // make sure spaces either side
        SpannableStringBuilder padSpaceLeft = new SpannableStringBuilder();
        SpannableStringBuilder padSpaceRight = new SpannableStringBuilder();
        if(before.length() > 0){
            char beforeChar = before.charAt(before.length()-1);
            if(beforeChar != ' ') {
                padSpaceLeft.append(" ");
            }
        }
        if(after.length() > 0){
            char afterChar = after.charAt(0);
            if(afterChar != ' ') {
                padSpaceRight.append(" ");
            }
        }
        editMessage.setText(TextUtils.concat(before,padSpaceLeft,newSpan,padSpaceRight,after));
        editMessage.setSelection(editMessage.length());

        dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_COMPARE_DATE,mCompareDate);
        outState.putString(KEY_SPEECH_FORMAT,mSpeechFormat);

        super.onSaveInstanceState(outState);
    }

    /**
     * Function to restore state data
     */
    protected void restoreStateData(@Nullable Bundle savedInstanceState){
        if(savedInstanceState != null){
            mCompareDate=savedInstanceState.getString(KEY_COMPARE_DATE);
            mSpeechFormat=savedInstanceState.getString(KEY_SPEECH_FORMAT);
        }

    }
}
