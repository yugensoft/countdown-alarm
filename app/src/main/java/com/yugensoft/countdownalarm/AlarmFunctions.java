package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;

import java.util.List;
import java.util.Locale;

public class AlarmFunctions {
    /**
     * Engage all active alarms, i.e. all active alarms get loaded into the AlarmManager ready to trigger
     * Does not disengage inactive alarms.
     * Assumes it is being called on application start or by the Boot Receiver
     */
    public static void engageAllAlarms(Context context, DaoSession daoSession, AlarmManager alarmManager){
        List<Alarm> alarms = daoSession.getAlarmDao().queryBuilder()
                .where(AlarmDao.Properties.Active.eq(true))
                .list();
        for (Alarm alarm : alarms){
            engageAlarm(alarm, context, daoSession, alarmManager);
        }
    }

    /**
     * Engage/disengage the given alarm based on active status
     */
    public static void engageAlarm(Alarm alarm, Context context, DaoSession daoSession, AlarmManager alarmManager){
        // check valid alarm passed
        if(alarm.getId() == null){
            throw new IllegalArgumentException("Attempt to engage invalid alarm");
        }
        if(alarm.getId() > Integer.MAX_VALUE){
            throw new RuntimeException("Design failure, alarm ID exceeded integer maximum");
        }

        // get snooze details
        long snoozeDuration = SettingsActivity.getSnoozeDurationInMs(context);

        long triggerTime = alarm.getNextAlarmTime().getTime();

        // prepare the message
        String message;
        if(alarm.getMessageId() == null) {
            message = null;
        } else {
            message = MessageActivity.renderTaggedText(
                    alarm.getMessage().getText(),
                    daoSession.getTagDao(),
                    context,
                    triggerTime
            ).toString();
        }

        // Create the pending intent
        Intent intent = AlarmReceiverActivity.newIntent(
                context,
                alarm.getId(),
                alarm.getRingtone(),
                alarm.getScheduleAlarmTime(context).humanReadable,
                triggerTime,
                snoozeDuration,
                alarm.getVibrate(),
                false,
                message
        );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent alarmIntent = PendingIntent.getActivity(
                context,
                alarm.getId().intValue(),
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        // Set or cancel the alarm depending on Active status
        if(alarm.getActive()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, alarmIntent);
        } else {
            alarmManager.cancel(alarmIntent);
        }
    }

}
