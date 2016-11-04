package com.yugensoft.countdownalarm;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import org.joda.time.DateTime;

public class TagInserterFragment extends DialogFragment {

    protected static final String KEY_CURSOR_POS = "cursor-pos";
    protected static final String KEY_MESSAGE_ID = "message-id";


    protected View mFragmentView;
    protected int mCursorPos;
    protected long mMessageId;

    protected Tag.TagType mTagType;
    protected String mCompareDate;
    protected String mSpeechFormat;

    public static String renderTag(Tag.TagType tagType, String speechFormat){
        String s;
        switch(tagType){
            case COUNTDOWN:
                //todo
                s="";
                break;
            case COUNTUP:
                //todo
                s="";
                break;
            case TODAYS_DATE:
                //todo complete
                DateTime now = DateTime.now();

                // Custom field 'dddd' is replaced manually with 1st,2nd,3rd etc
                String speechFormatExt = speechFormat.replace(
                        "dddd",
                        "'" + String.valueOf(now.getDayOfMonth()) + MiscFunctions.getLastDigitSufix(now.getDayOfMonth()) + "'"
                );
                s = now.toString(speechFormatExt);
                break;
            default:
                throw new RuntimeException("Unknown tag type");
        }

        return s;
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
        String tagRendering = renderTag(mTagType,mSpeechFormat);

        // insert span into edittext
        EditText editMessage = (EditText)getActivity().findViewById(R.id.edit_message);
        Spannable str = editMessage.getText();
        Spannable before = new SpannableStringBuilder(str,0,mCursorPos);
        Spannable after = new SpannableStringBuilder(str,mCursorPos,str.length());
        Spannable newSpan = new SpannableStringBuilder(tagRendering);
        //todo: colors
        newSpan.setSpan(new TagSpan(Color.RED, Color.GREEN, tag), 0, newSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        editMessage.setSelection(mCursorPos + tagRendering.length() + padSpaceRight.length());

        dismiss();
    }

}
