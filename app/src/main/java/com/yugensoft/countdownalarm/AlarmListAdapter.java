package com.yugensoft.countdownalarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.support.v7.widget.PopupMenu;
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
import android.widget.TextView;

import org.greenrobot.greendao.async.AsyncOperation;
import org.greenrobot.greendao.async.AsyncOperationListener;
import org.greenrobot.greendao.async.AsyncSession;
import org.greenrobot.greendao.query.Query;

import java.util.Collections;
import java.util.List;

import static com.yugensoft.countdownalarm.AlarmFunctions.getAllAlarmsSorted;
import static com.yugensoft.countdownalarm.AlarmFunctions.getNextAlarmTimeReadable;
import static com.yugensoft.countdownalarm.AlarmFunctions.previewAlarm;
import static com.yugensoft.countdownalarm.AlarmFunctions.setNotification;


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
        ImageView wMessageIcon = (ImageView)convertView.findViewById(R.id.iv_message);
        TextView wMessageText = (TextView) convertView.findViewById(R.id.text_message);
        ImageButton wMenu = (ImageButton)convertView.findViewById(R.id.ib_menu);

        /**
         * populate data & setup controls
         */
        // checkbox to activate
        wActive.setOnCheckedChangeListener(null);
        wActive.setChecked(alarm.getActive());
        wActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AlarmActivenessChanged(isChecked, alarm);
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
                                previewAlarm(alarm,mActivity,mDaoSession);
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
        String scheduleText = alarm.getScheduleRepeatDays(mActivity.getResources()).humanReadable;
        scheduleText = scheduleText.equals(mActivity.getString(R.string.never)) ? "" : scheduleText;
        wAlarmSchedule.setText(scheduleText);
        if(scheduleText.length() == 0) {
            wAlarmSchedule.setVisibility(View.GONE);
        } else {
            wAlarmSchedule.setVisibility(View.VISIBLE);
        }
        // label
        wAlarmLabel.setText(alarm.getLabel());
        // message
        if (alarm.getMessageId() == null) {
            wMessageIcon.setVisibility(View.GONE);
            wMessageText.setText("");
            wMessageText.setVisibility(View.GONE);
        } else {
            wMessageIcon.setVisibility(View.VISIBLE);
            String text = alarm.getMessage().getText();
            text = "\"" + MessageActivity.renderTaggedText(text, mDaoSession.getTagDao(), mActivity, alarm.getNextAlarmTime().getTime()).toString() + "\"";
            wMessageText.setText(text);
            wMessageText.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    /**
     * Method to perform all actions required when an alarm's activeness is changed.
     * @param isChecked The users new intended activeness of the alarm.
     * @param alarm The alarm.
     */
    private void AlarmActivenessChanged(boolean isChecked, Alarm alarm) {
        // Update the activeness in the DB
        alarm.setActive(isChecked);
        alarm.update();
        // Reset the corresponding alarm service according to the activeness
        AlarmFunctions.engageAlarm(
                alarm,
                mActivity,
                mDaoSession,
                (AlarmManager)mActivity.getSystemService(Context.ALARM_SERVICE)
        );
        // Display the new alarm set time as a Toast
        if(isChecked) {
            TimeFormatters.getAlarmSetTime(alarm.getNextAlarmTime(),true, mActivity, true);
        }
        // Update the next alarm textview
        String nextAlarmTime = getNextAlarmTimeReadable(mAlarms, mActivity);
        mTextNextAlarm.setText(nextAlarmTime);
        // Update the Android User Notification thing of the next alarm
        setNotification(nextAlarmTime,mActivity,mActivity.getString(R.string.app_name));
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
        mDaoSession.getAlarmDao().delete(alarm);
        updateAlarms();
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
        AlarmFunctions.GetAlarmsCallback callback = new AlarmFunctions.GetAlarmsCallback() {
            @Override
            public void callback(List<Alarm> alarms) {
                mAlarms = alarms;

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                        String nextAlarmTime = getNextAlarmTimeReadable(mAlarms, mActivity);
                        mTextNextAlarm.setText(nextAlarmTime);
                        setNotification(nextAlarmTime,mActivity,mActivity.getString(R.string.app_name));
                    }
                });
            }
        };

        getAllAlarmsSorted(mDaoSession,callback);

    }


}
