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
     * @param toastContext Context to display a Toast in; no toast if null
     */
    public static void getNextAlarmTime(Date nextAlarmTime, boolean addAffixes, @Nullable Context toastContext){
        StringBuilder sb = new StringBuilder();
        if(addAffixes) {
            sb.append("Alarm is set for ");
        }

        DateTime dateTime = new DateTime(nextAlarmTime);
        Period period = new Period(DateTime.now(),dateTime);
        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendDays()
                .appendSuffix(" ").appendSuffix("days").appendSeparator(" ")
                .appendHours()
                .appendSuffix(" ").appendSuffix("hours").appendSeparator(" ")
                .printZeroAlways()
                .appendMinutes()
                .appendSuffix(" ").appendSuffix("minutes").appendSeparator(" ","")
                .toFormatter();
        sb.append(formatter.print(period));
        if(addAffixes) {
            sb.append(" from now.");
        }
        String output = sb.toString();

        if(toastContext != null) {
            Toast.makeText(toastContext, output, Toast.LENGTH_LONG).show();
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
