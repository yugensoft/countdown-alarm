package com.yugensoft.countdownalarm;

import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;

public class AlarmTimeFormatter {

    /**
     * Get the next alarm time in a human readable format
     * @param nextAlarmTime The next alarm time in Date format
     * @param addAffixes Add explanatory strings. Make false to get alarm time only.
     * @param context Needed for res strings and toasts.
     * @param showToast True to show toasts.
     */
    public static void getNextAlarmTime(Date nextAlarmTime, boolean addAffixes, Context context, boolean showToast){
        StringBuilder sb = new StringBuilder();
        if(addAffixes) {
            sb.append(context.getString(R.string.alarm_set_for));
            sb.append(" ");
        }

        DateTime dateTime = new DateTime(nextAlarmTime);
        Period period = new Period(DateTime.now(),dateTime);
        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendDays()
                .appendSuffix(" ").appendSuffix(context.getString(R.string.days)).appendSeparator(" ")
                .appendHours()
                .appendSuffix(" ").appendSuffix(context.getString(R.string.hours)).appendSeparator(" ")
                .printZeroAlways()
                .appendMinutes()
                .appendSuffix(" ").appendSuffix(context.getString(R.string.minutes)).appendSeparator(" ","")
                .toFormatter();
        sb.append(formatter.print(period));
        if(addAffixes) {
            sb.append(" ");
            sb.append(context.getString(R.string.from_now));
        }
        String output = sb.toString();

        if(showToast) {
            Toast.makeText(context, output, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Functions to convert various time variable formats into a readable hours/minutes string
     */
    public static String convertTimeToReadable(Date date, Context context){
        DateTime dateTime = new DateTime(date);
        return dateTimeToReadableHoursMinutesConverter(dateTime, context);
    }
    public static String convertTimeToReadable(int hour, int minute, Context context){
        DateTime dateTime = new DateTime(2000,1,1,hour,minute);
        return dateTimeToReadableHoursMinutesConverter(dateTime, context);
    }

    private static String dateTimeToReadableHoursMinutesConverter(DateTime dateTime, Context context){
        DateTimeFormatter fmt;
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            fmt = DateTimeFormat.forPattern("HH:mm");
        } else {
            fmt = DateTimeFormat.forPattern("hh:mm a");
        }
        return fmt.print(dateTime);
    }

}
