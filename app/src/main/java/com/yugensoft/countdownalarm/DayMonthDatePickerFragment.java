package com.yugensoft.countdownalarm;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.Calendar;


public class DayMonthDatePickerFragment extends DialogFragment implements DayMonthDatePickerDialog.OnDateSetListener {
    private Tracker mTracker;

    public static abstract class PickerCallback {
        public abstract void callback(int month, int day);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication)getActivity().getApplication()).getDefaultTracker();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        // Create a new instance of TimePickerDialog and return it
        DayMonthDatePickerDialog dialog = new DayMonthDatePickerDialog(getActivity(), month, day);
        dialog.setTitle("Set date");
        dialog.setDateSetListener(this);
        return dialog;

    }

    @Override
    public void onDateSet(int month, int dayOfMonth) {
        int monthOneBased = month + 1; // The Calender starts months at 0, but the TimeDate classes start at 1

        // Call the callbackSet function, to cause a notifySetDataChanged back in the activity
        if(mPickerCallback !=null) {
            mPickerCallback.callback(monthOneBased, dayOfMonth);
        } else {
            throw new RuntimeException("Callback must be set");
        }

        Toast.makeText(
                getActivity(),
                "Date Updated",
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
