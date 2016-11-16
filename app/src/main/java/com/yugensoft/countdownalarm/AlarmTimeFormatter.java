package com.yugensoft.countdownalarm;

import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;

public class AlarmTimeFormatter {

    /**
     * Get the next alarm time in a human readable format
     * @param nextAlarmTime The next alarm time in Date format
     * @param toastContext Context to display a Toast in; no toast if null
     */
    public static void getNextAlarmTime(Date nextAlarmTime, @Nullable Context toastContext){
        StringBuilder sb = new StringBuilder("Alarm is set for ");
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
        sb.append(formatter.print(period)).append(" from now.");
        String output = sb.toString();

        if(toastContext != null) {
            Toast.makeText(toastContext, output, Toast.LENGTH_LONG).show();
        }
    }

}
