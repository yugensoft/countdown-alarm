package com.yugensoft.countdownalarm;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import java.lang.reflect.Field;
import java.text.DateFormatSymbols;
import java.util.Calendar;


public class DayMonthDatePickerDialog extends AlertDialog implements DialogInterface.OnClickListener {

    private static final String MONTH = "month";
    private static final String DAY = "day";
    private OnDateSetListener mDateSetListener;

    // views
    private NumberPicker mMonthPicker;
    private NumberPicker mDayPicker;

    protected DayMonthDatePickerDialog(Context context, int monthOfYear, int dayOfMonth) {
        super(context);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_day_month_picker, null);
        setView(view);

        setButton(BUTTON_POSITIVE, context.getString(R.string.set), this);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), this);

        mMonthPicker = (NumberPicker)view.findViewById(R.id.picker_month);
        mDayPicker = (NumberPicker)view.findViewById(R.id.picker_day);

        // Display months
        mMonthPicker.setMinValue(0);
        mMonthPicker.setMaxValue(11);
        String[] shortMonths = new DateFormatSymbols().getShortMonths();
        mMonthPicker.setDisplayedValues(shortMonths);
        mMonthPicker.setValue(monthOfYear);
        mMonthPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setDayRangeByMonth();
            }
        });
        setDayRangeByMonth();
        mDayPicker.setValue(dayOfMonth);
    }

    private void setDayRangeByMonth(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH,mMonthPicker.getValue());
        int lastDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        mDayPicker.setMinValue(1);
        mDayPicker.setMaxValue(lastDay);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(which){
            case BUTTON_POSITIVE:
                if(mDateSetListener != null) {
                    mDayPicker.clearFocus();
                    mMonthPicker.clearFocus();
                    mDateSetListener.onDateSet(mMonthPicker.getValue(),mDayPicker.getValue());
                }
                break;
            case BUTTON_NEGATIVE:
                cancel();
                break;
            default:
                throw new RuntimeException("Unexpected thing clicked.");
        }
    }

    public OnDateSetListener getDateSetListener() {
        return mDateSetListener;
    }

    public void setDateSetListener(OnDateSetListener mDateSetListener) {
        this.mDateSetListener = mDateSetListener;
    }

    @Override
    public Bundle onSaveInstanceState() {
        final Bundle state = super.onSaveInstanceState();
        state.putInt(MONTH, mMonthPicker.getValue());
        state.putInt(DAY, mDayPicker.getValue());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int month = savedInstanceState.getInt(MONTH);
        final int day = savedInstanceState.getInt(DAY);
        mMonthPicker.setValue(month);
        setDayRangeByMonth();
        mDayPicker.setValue(day);
    }

    /**
     * The listener used to indicate the user has finished selecting a date.
     */
    public interface OnDateSetListener {
        /**
         * @param month the selected month (0-11 for compatibility with
         *              {@link Calendar#MONTH})
         * @param dayOfMonth th selected day of the month (1-31, depending on
         *                   month)
         */
        void onDateSet(int month, int dayOfMonth);
    }
}