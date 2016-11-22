package com.yugensoft.countdownalarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.greendao.async.AsyncOperation;
import org.greenrobot.greendao.async.AsyncOperationListener;
import org.greenrobot.greendao.async.AsyncSession;
import org.greenrobot.greendao.query.Query;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;


public class AlarmListAdapter extends BaseAdapter {
    private static String TAG = "app-debug";

    private Activity mActivity;
    private LayoutInflater mInflater;
    private DaoSession mDaoSession;
    private List<Alarm> mAlarms;

    private TextView mTextNextAlarm;

    public AlarmListAdapter(Activity activity, DaoSession daoSession) {
        mActivity = activity;
        mDaoSession = daoSession;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTextNextAlarm = (TextView) activity.findViewById(R.id.text_next_alarm);
    }

    @Override
    public int getCount() {
        if(mAlarms == null) {
            return 0;
        } else {
            return mAlarms.size();
        }
    }

    @Override
    public Object getItem(int position) {
        return mAlarms.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mAlarms.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.alarm_list_row, null);
        }

        // check if has no alarms
        if(mAlarms == null) {
            return null;
        }

        final Alarm alarm = mAlarms.get(position);
        final long alarmId = alarm.getId();
        // views
        CheckBox wActive = (CheckBox)convertView.findViewById(R.id.checkbox_active);
        TextView wAlarmTime = (TextView)convertView.findViewById(R.id.text_time);
        TextView wAlarmSchedule = (TextView)convertView.findViewById(R.id.text_alarm_schedule);
        TextView wAlarmLabel = (TextView)convertView.findViewById(R.id.text_alarm_label);
        ImageView wMessage = (ImageView)convertView.findViewById(R.id.iv_message);
        ImageButton wMenu = (ImageButton)convertView.findViewById(R.id.ib_menu);
        // layouts
        LinearLayout llAlarmSchedule= (LinearLayout)convertView.findViewById(R.id.ll_alarm_schedule);
        LinearLayout llAlarmLabel = (LinearLayout)convertView.findViewById(R.id.ll_alarm_label);

        /**
         * populate data & setup controls
         */
        // checkbox to activate
        wActive.setOnCheckedChangeListener(null);
        wActive.setChecked(alarm.getActive());
        wActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "onCheckedChanged: " + String.valueOf(alarm.getId()) + "," + String.valueOf(isChecked));
                alarm.setActive(isChecked);
                alarm.update();
                AlarmFunctions.engageAlarm(
                        alarm,
                        mActivity,
                        mDaoSession,
                        (AlarmManager)mActivity.getSystemService(Context.ALARM_SERVICE)
                );
                if(isChecked) {
                    AlarmTimeFormatter.getNextAlarmTime(alarm.getNextAlarmTime(),true, mActivity);
                }
                updateNextAlarm();
            }
        });
        // alarm time
        wAlarmTime.setText(alarm.getScheduleAlarmTime(mActivity).humanReadable);
        // dropdown menu

        wMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(mActivity, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()){
                            case R.id.mi_preview:
                                previewAlarm(alarm);
                                return true;
                            case R.id.mi_delete:
                                deleteAlarm(alarm);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_alarm, popup.getMenu());
                popup.show();

            }
        });
        // schedule
        String scheduleText = alarm.getScheduleRepeatDays().humanReadable;
        scheduleText = scheduleText.equals("Never") ? "" : scheduleText;
        wAlarmSchedule.setText(scheduleText);
        // label
        wAlarmLabel.setText(alarm.getLabel());
        if(alarm.getMessageId() == null) {
            wMessage.setVisibility(View.INVISIBLE);
        }else{
            wMessage.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    private void deleteAlarm(Alarm alarm) {
        // first deactivate it and disengage it
        alarm.setActive(false);
        AlarmFunctions.engageAlarm(
                alarm,
                mActivity,
                mDaoSession,
                (AlarmManager)mActivity.getSystemService(Context.ALARM_SERVICE)
        );
        // delete it
        Log.d(TAG, "deleteAlarm: " + String.valueOf(alarm.getId()));
        mDaoSession.getAlarmDao().delete(alarm);
        updateAlarms();
    }

    private void previewAlarm(Alarm alarm) {
        // get snooze details
        long snoozeDuration = SettingsActivity.getSnoozeDurationInMs(mActivity);

        // get alarm and pass details to alarm-reciever
        String message;
        if(alarm.getMessageId() == null) {
            message = null;
        } else {
            message = MessageActivity.renderTaggedText(
                    alarm.getMessage().getText(),
                    mDaoSession.getTagDao(),
                    mActivity
            ).toString();
        }
        Intent intent = AlarmReceiverActivity.newIntent(
                mActivity,
                alarm.getId(),
                alarm.getRingtone(),
                alarm.getScheduleAlarmTime(mActivity).humanReadable,
                new Date().getTime(),
                snoozeDuration,
                alarm.getVibrate(),
                true,
                message
        );

        mActivity.startActivity(intent);
    }

    /**
     * Function to update all the alarms, which means:
     * - query the alarms
     * - sort them by scheduled time
     * - save that data into this adapter
     * - determine the next alarm, if any
     * -- set an android notification
     * - update the views
     */
    public void updateAlarms(){
        mDaoSession.clear();
        Query alarmQuery = mDaoSession.getAlarmDao().queryBuilder().build();
        AsyncSession asyncSession = mDaoSession.startAsyncSession();

        asyncSession.setListener(new AsyncOperationListener() {
            @Override
            public void onAsyncOperationCompleted(AsyncOperation operation) {
                List<Alarm>alarmsToSort = (List<Alarm>)operation.getResult();

                // sort the alarms by scheduled time
                AlarmScheduleTimeComparator comparator = new AlarmScheduleTimeComparator();
                Collections.sort(alarmsToSort,comparator);
                mAlarms = alarmsToSort;

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                        updateNextAlarm();
                    }
                });

            }
        });
        asyncSession.queryList(alarmQuery);

    }

    /**
     * Update information as to when the next alarm will run
     */
    public void updateNextAlarm(){
        List<Alarm> alarmsCopy = new ArrayList<Alarm>(mAlarms);

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
            mNextAlarm = "Next alarm is at " + AlarmTimeFormatter.convertTimeToReadable(nextAlarm, mActivity);
        } else {
            mNextAlarm = "";
        }

        mTextNextAlarm.setText(mNextAlarm);
        setNotification(mNextAlarm);
    }

    /**
     * Function to create an android notification for the existence of a pending alarm
     * @param text The alarm information
     */
    private void setNotification(String text) {
        NotificationCompat.Builder builder =
                (android.support.v7.app.NotificationCompat.Builder)
                new NotificationCompat.Builder(mActivity)
                .setSmallIcon(R.drawable.ic_action_alarm)
                .setContentTitle(mActivity.getTitle())
                .setContentText(text);
        // Make the notification bring the user back to the main activity on click
        Intent resultIntent = new Intent(mActivity, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        mActivity,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        // Sets an ID for the notification
        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) mActivity.getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it, or cancels it if blank
        if(text.isEmpty()) {
            mNotifyMgr.cancel(mNotificationId);
        } else {
            mNotifyMgr.notify(mNotificationId, builder.build());
        }
    }
}
