package com.yugensoft.countdownalarm;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Months;

/**
 * Created by yugensoft on 4/11/2016.
 */
public class CountingTagInserterFragment extends TagInserterFragment {

    public enum CountingDirection {
        UP, DOWN;
    }
    private static final String KEY_COUNTING_DIRECTION = "key-counting-direction";

    private Button mCancelButton;
    private Button mInsertButton;
    private RadioGroup mRadioGroup;
    private TextView mComparisonDateLabel;
    private TextView mComparisonDateText;

    public static CountingTagInserterFragment newInstance(int cursorPos, long messageId, CountingDirection countingDirection) {
        CountingTagInserterFragment f = new CountingTagInserterFragment();
        Bundle args = new Bundle();

        args.putInt(KEY_CURSOR_POS, cursorPos);
        args.putLong(KEY_MESSAGE_ID, messageId);
        args.putSerializable(KEY_COUNTING_DIRECTION, countingDirection);

        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (getArguments().getSerializable(KEY_COUNTING_DIRECTION) == CountingDirection.UP) {
            dialog.setTitle("Insert Countup Tag");
        } else {
            dialog.setTitle("Insert Countdown Tag");
        }
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // layouts and widgets
        mFragmentView = inflater.inflate(R.layout.fragment_counting_tag_inserter, container, false);
        mCancelButton=(Button)mFragmentView.findViewById(R.id.button_ti_cancel);
        mInsertButton=(Button)mFragmentView.findViewById(R.id.button_ti_insert);
        mRadioGroup=(RadioGroup)mFragmentView.findViewById(R.id.rg_1);
        mComparisonDateLabel=(TextView)mFragmentView.findViewById(R.id.label_compare_date);
        mComparisonDateText=(TextView)mFragmentView.findViewById(R.id.text_compare_date);

        // Get and process arguments & fields
        Bundle args = getArguments();
        mCursorPos = args.getInt(KEY_CURSOR_POS);
        mMessageId = args.getLong(KEY_MESSAGE_ID);
        CountingDirection countingDirection = (CountingDirection)args.getSerializable(KEY_COUNTING_DIRECTION);
        if(countingDirection == CountingDirection.UP) {
            mTagType = Tag.TagType.COUNTUP;
            mComparisonDateLabel.setText(R.string.count_up_to);
        } else {
            mTagType = Tag.TagType.COUNTDOWN;
            mComparisonDateLabel.setText(R.string.count_down_to);
        }


        // click listeners
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mInsertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    insertTag();
                } catch (TagPropertyMissingException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = (RadioButton)mFragmentView.findViewById(checkedId);
                mSpeechFormat = rb.getTag().toString(); // tags contain the speech format
            }
        });
        mComparisonDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DayMonthDatePickerFragment fragment = DayMonthDatePickerFragment.newInstance(new DayMonthDatePickerFragment.PickerCallback(){
                    @Override
                    public void callback(int year, int month, int day) {
                        DateTime dt = new DateTime(year, month, day, 0, 0);
                        mCompareDate = dt.toString(COMPARE_DATE_STORAGE_FORMAT);
                        mComparisonDateText.setText(dt.toString("MMM dd"));
                        // fill radiobuttons with text
                        mComparisonDateLabel.setVisibility(View.VISIBLE);
                        mRadioGroup.setVisibility(View.VISIBLE);
                        for (int i = 0; i < mRadioGroup.getChildCount(); i++){
                            RadioButton rb = (RadioButton)mRadioGroup.getChildAt(i);
                            rb.setText(renderTag(mTagType,rb.getTag().toString(),mCompareDate,true));
                       }
                    }
                });
                fragment.show(getActivity().getFragmentManager(),"day-month-date-picker");
            }
        });

        return mFragmentView;
    }
}