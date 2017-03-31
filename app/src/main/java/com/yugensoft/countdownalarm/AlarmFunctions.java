package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.NotificationCompat;

import org.greenrobot.greendao.async.AsyncOperation;
import org.greenrobot.greendao.async.AsyncOperationListener;
import org.greenrobot.greendao.async.AsyncSession;
import org.greenrobot.greendao.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;

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

    /**
     * Play alarm immediately as a preview.
     * @param alarm The alarm to play.
     */
    public static void previewAlarm(Alarm alarm, Context context, DaoSession daoSession) {
        // get snooze details
        long snoozeDuration = SettingsActivity.getSnoozeDurationInMs(context);

        // get alarm and pass details to alarm-reciever
        String message;
        if(alarm.getMessageId() == null) {
            message = null;
        } else {
            message = MessageActivity.renderTaggedText(
                    alarm.getMessage().getText(),
                    daoSession.getTagDao(),
                    context,
                    null
            ).toString();
        }
        Intent intent = AlarmReceiverActivity.newIntent(
                context,
                alarm.getId(),
                alarm.getRingtone(),
                alarm.getScheduleAlarmTime(context).humanReadable,
                new Date().getTime(),
                snoozeDuration,
                alarm.getVibrate(),
                true,
                message
        );

        context.startActivity(intent);
    }

    /**
     * Get information as to when the next alarm will run
     */
    public static String getNextAlarmTimeReadable(List<Alarm> alarms, Context context){
        List<Alarm> alarmsCopy = new ArrayList<Alarm>(alarms);

        // remove any inactive alarms
        for(Iterator<Alarm> iter = alarmsCopy.iterator(); iter.hasNext();){
            Alarm alarm = iter.next();
            if(!alarm.getActive()){
                iter.remove();
            }
        }

        // get the next alarm that will go off
        String mNextAlarm;
        if(alarmsCopy.size() > 0) {
            Date nextAlarm = Collections.min(alarmsCopy, new AlarmNextTimeComparator()).getNextAlarmTime();
            mNextAlarm = context.getString(R.string.next_alarm_at) + " " + TimeFormatters.convertTimeToReadable(nextAlarm, context);
        } else {
            mNextAlarm = "";
        }

        return mNextAlarm;
    }

    /**
     * Method to create an android notification for the existence of a pending alarm
     * @param text The alarm information. Cancels notification if blank.
     */
    public static void setNotification(String text, Context context, String title) {
        NotificationCompat.Builder builder =
                (android.support.v7.app.NotificationCompat.Builder)
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.ic_action_alarm)
                                .setContentTitle(title)
                                .setContentText(text);
        // Make the notification bring the user back to the main activity on click
        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        // Sets an ID for the notification
        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it, or cancels it if blank
        if(text.isEmpty()) {
            mNotifyMgr.cancel(mNotificationId);
        } else {
            mNotifyMgr.notify(mNotificationId, builder.build());
        }
    }

    /**
     * Function to asynchronously return a time sorted list of the Alarms
     * @param daoSession
     * @param resultCallback
     */
    public static void getAllAlarmsSorted(DaoSession daoSession, final GetAlarmsCallback resultCallback){
        daoSession.clear();
        Query alarmQuery = daoSession.getAlarmDao().queryBuilder().build();
        AsyncSession asyncSession = daoSession.startAsyncSession();

        asyncSession.setListener(new AsyncOperationListener() {
            @Override
            public void onAsyncOperationCompleted(AsyncOperation operation) {
                List<Alarm>alarmsToSort = (List<Alarm>)operation.getResult();

                // sort the alarms by scheduled time
                AlarmScheduleTimeComparator comparator = new AlarmScheduleTimeComparator();
                Collections.sort(alarmsToSort,comparator);

                // return them
                resultCallback.callback(alarmsToSort);
            }
        });
        asyncSession.queryList(alarmQuery);
    }
    public static abstract class GetAlarmsCallback{
        public abstract void callback(List<Alarm> alarms);
    }

}
