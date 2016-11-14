package com.yugensoft.countdownalarm;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import java.util.Calendar;


public class DayMonthDatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    public static abstract class PickerCallback {
        public abstract void callback(int year, int month, int day);
    }
    private PickerCallback mPickerCallback;
    public void setPickerCallback(PickerCallback pickerCallback) {
        this.mPickerCallback = pickerCallback;
    }

    public static DayMonthDatePickerFragment newInstance(PickerCallback pickerCallback){

        DayMonthDatePickerFragment fragment = new DayMonthDatePickerFragment();
        fragment.setPickerCallback(pickerCallback);
        return fragment;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        // Create a new instance of TimePickerDialog and return it
        return new DayMonthDatePickerDialog(getActivity(), this, year, month, day);

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        int monthOneBased = month + 1; // The Calender/DatePickerDialog starts months at 0, but the TimeDate classes start at 1

        // Call the callbackSet function, to cause a notifySetDataChanged back in the activity
        if(mPickerCallback !=null) {
            mPickerCallback.callback(year, monthOneBased, dayOfMonth);
        } else {
            throw new RuntimeException("Callback must be set");
        }

        Toast.makeText(
                getActivity(),
                "Date Updated",
                Toast.LENGTH_LONG
        ).show();
    }
}
