package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;

/**
 * Functions to convert various time variable formats into a readable hours/minutes string
 */
public class TimeFormatters {

    /**
     * Get the alarm set time in a human readable format
     * @param alarmSetTime The next alarm time in Date format
     * @param addAffixes Add explanatory strings. Make false to get alarm time only.
     * @param context Needed for res strings and toasts.
     * @param showToast True to show toasts.
     */
    public static String getAlarmSetTime(Date alarmSetTime, boolean addAffixes, Context context, boolean showToast){
        StringBuilder sb = new StringBuilder();
        if(addAffixes) {
            sb.append(context.getString(R.string.alarm_set_for));
            sb.append(" ");
        }

        DateTime dateTime = new DateTime(alarmSetTime);
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

        return output;
    }

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

    /**
     * Convert a number of days, by division & remainder, into a weeks/months/days etc representation.
     * @param resources Resource object to get string resources.
     * @param daysBetween Duration in days.
     * @param speechFormat Format for how to render into a phrase.
     * @param prependFormat Whether or not to prepend format-explaining text like 'Months Weeks Days' etc to the representation.
     * @return String representation of time duration
     */
    public static String convertDaysToSpeechFormat(
            Resources resources,
            Days daysBetween,
            String speechFormat,
            @Nullable Boolean prependFormat
    ){
        final double DAYS_IN_A_MONTH = 30.42;
        final double DAYS_IN_A_WEEK = 7;

        String output;
        int months, weeks, days;
        int prependString;

        switch(speechFormat){
            case "dd":
                output = resources.getQuantityString(R.plurals.days,daysBetween.getDays(),daysBetween.getDays());
                prependString = R.string.fmt_days;
                break;
            case "MM dd":
                months = (int)(daysBetween.getDays() / DAYS_IN_A_MONTH);
                days = (int)(daysBetween.getDays() % DAYS_IN_A_MONTH);
                output = "";
                if (months > 0) {
                    output += resources.getQuantityString(R.plurals.months,months,months) + " ";
                }
                if (days > 0 || months == 0){
                    output += resources.getQuantityString(R.plurals.days,days,days);
                }
                prependString = R.string.fmt_months_days;
                break;
            case "ww dd":
                weeks = (int)(daysBetween.getDays() / DAYS_IN_A_WEEK);
                days = (int)(daysBetween.getDays() % DAYS_IN_A_WEEK);
                output = "";
                if (weeks > 0) {
                    output += resources.getQuantityString(R.plurals.weeks,weeks,weeks) + " ";
                }
                if (days > 0 || weeks == 0){
                    output += resources.getQuantityString(R.plurals.days,days,days);
                }
                prependString = R.string.fmt_weeks_days;
                break;
            case "MM ww dd":
                months = (int)(daysBetween.getDays() / DAYS_IN_A_MONTH);
                int daysRemainder = (int)(daysBetween.getDays() % DAYS_IN_A_MONTH);
                output = "";
                if (months > 0) {
                    output += resources.getQuantityString(R.plurals.months,months,months) + " ";
                }
                weeks = (int)(daysRemainder / DAYS_IN_A_WEEK);
                days = (int)(daysRemainder % DAYS_IN_A_WEEK);
                if (weeks > 0) {
                    output += resources.getQuantityString(R.plurals.weeks,weeks,weeks) + " ";
                }
                if (days > 0 || (weeks == 0 && months == 0)){
                    output += resources.getQuantityString(R.plurals.days,days,days);
                }
                prependString = R.string.fmt_months_weeks_days;
                break;
            default:
                throw new RuntimeException("Unknown countdown speech format");
        }
        // Add prepend text if requested
        if(prependFormat != null && prependFormat){
            output = resources.getString(prependString) + " (" + output + ")";
        }
        return output;
    }
}
