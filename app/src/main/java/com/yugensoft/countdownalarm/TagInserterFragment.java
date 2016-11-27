package com.yugensoft.countdownalarm;

import android.app.Dialog;
import android.graphics.Color;
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
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class TagInserterFragment extends DialogFragment {

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

    public static String renderTag(
            Tag.TagType tagType,
            String speechFormat,
            @Nullable String comparisonDate,
            @Nullable Boolean prependFormat
    ){
        String renderedOutput;
        DateTime now = DateTime.now();

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

                renderedOutput = convertDaysToFormat(daysBetween,speechFormat,prependFormat);

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

                renderedOutput = convertDaysToFormat(daysBetween,speechFormat,prependFormat);

                break;
            case TODAYS_DATE:
                // Custom field 'dddd' is replaced manually with 1st,2nd,3rd etc
                String speechFormatExt = speechFormat.replace(
                        "dddd",
                        "'" + String.valueOf(now.getDayOfMonth()) + MiscFunctions.getLastDigitSufix(now.getDayOfMonth()) + "'"
                );
                renderedOutput = now.toString(speechFormatExt);
                break;
            default:
                throw new RuntimeException("Unknown tag type");
        }

        return renderedOutput;
    }

    public static String convertDaysToFormat(Days daysBetween, String speechFormat, @Nullable Boolean prependFormat){
        String output;
        int months, weeks, days;
        switch(speechFormat){
            case "dd":
                output = daysBetween.getDays() + " days";
                if(prependFormat != null && prependFormat){
                    output = String.format("Days (%s)",output);
                }
                break;
            case "MM dd":
                months = daysBetween.getDays() / 30;
                days = daysBetween.getDays() % 30;
                output = "";
                if (months > 0) {
                    output += String.valueOf(months) + " months ";
                }
                if (days > 0 || months == 0){
                    output += String.valueOf(days) + " days";
                }
                if(prependFormat != null && prependFormat){
                    output = String.format("Months Days (%s)",output);
                }
                break;
            case "ww dd":
                weeks = daysBetween.getDays() / 7;
                days = daysBetween.getDays() % 7;
                output = "";
                if (weeks > 0) {
                    output += String.valueOf(weeks) + " weeks ";
                }
                if (days > 0 || weeks == 0){
                    output += String.valueOf(days) + " days";
                }
                if(prependFormat != null && prependFormat){
                    output = String.format("Weeks Days (%s)",output);
                }
                break;
            case "MM ww dd":
                months = daysBetween.getDays() / 30;
                int daysRemainder = daysBetween.getDays() % 30;
                output = "";
                if (months > 0) {
                    output += String.valueOf(months) + " months ";
                }
                weeks = daysRemainder / 7;
                days = daysRemainder % 7;
                if (weeks > 0) {
                    output += String.valueOf(weeks) + " weeks ";
                }
                if (days > 0 || (weeks == 0 && months == 0)){
                    output += String.valueOf(days) + " days";
                }
                if(prependFormat != null && prependFormat){
                    output = String.format("Months Weeks Days (%s)",output);
                }
                break;
            default:
                throw new RuntimeException("Unknown countdown speech format");

        }
        return output;
    }

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
                throw new TagPropertyMissingException("Please choose a date!");
            }
        }
        if(mSpeechFormat != null) {
            tag.setSpeechFormat(mSpeechFormat);
        } else {
            throw new TagPropertyMissingException("Please choose a speech format!");
        }

        // insert into tags db table
        DaoSession daoSession = ((CountdownAlarmApplication)getActivity().getApplication()).getDaoSession();
        TagDao tagDao = daoSession.getTagDao();
        long tagId = tagDao.insert(tag);

        // generate textual representation of the tag
        String tagRendering = renderTag(mTagType,mSpeechFormat,mCompareDate,null);

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
        Spannable padSpaceLeft, padSpaceRight;
        // todo: not working, always adding spaces
        if(before.length() > 0 && before.subSequence(before.length()-1,before.length()) != " ") {
            padSpaceLeft = new SpannableStringBuilder(" ");
        } else {
            padSpaceLeft = new SpannableStringBuilder("");
        }
        if(after.length() > 0 && after.subSequence(0,1) != " ") {
            padSpaceRight = new SpannableStringBuilder(" ");
        } else {
            padSpaceRight = new SpannableStringBuilder("");
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
