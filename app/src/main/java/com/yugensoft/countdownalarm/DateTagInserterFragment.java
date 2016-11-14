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
import android.widget.Toast;

/**
 * Created by yugensoft on 4/11/2016.
 */
public class DateTagInserterFragment extends TagInserterFragment {
    private Button mCancelButton;
    private Button mInsertButton;
    private RadioGroup mRadioGroup;

    public static DateTagInserterFragment newInstance(int cursorPos, long messageId) {
        DateTagInserterFragment f = new DateTagInserterFragment();
        Bundle args = new Bundle();

        args.putInt(KEY_CURSOR_POS, cursorPos);
        args.putLong(KEY_MESSAGE_ID, messageId);

        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Insert Date Tag");
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // layouts and widgets
        mFragmentView = inflater.inflate(R.layout.fragment_date_tag_inserter, container, false);
        mCancelButton=(Button)mFragmentView.findViewById(R.id.button_ti_cancel);
        mInsertButton=(Button)mFragmentView.findViewById(R.id.button_ti_insert);
        mRadioGroup=(RadioGroup)mFragmentView.findViewById(R.id.rg_1);

        // Get and process arguments & fields
        Bundle args = getArguments();
        mCursorPos = args.getInt(KEY_CURSOR_POS);
        mMessageId = args.getLong(KEY_MESSAGE_ID);
        mTagType= Tag.TagType.TODAYS_DATE;

        // fill radiobuttons with text
        for (int i = 0; i < mRadioGroup.getChildCount(); i++){
            RadioButton rb = (RadioButton)mRadioGroup.getChildAt(i);
            rb.setText(renderTag(mTagType,rb.getTag().toString(),null,null));
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

        return mFragmentView;
    }
}
