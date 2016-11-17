package com.yugensoft.countdownalarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.greendao.async.AsyncOperation;
import org.greenrobot.greendao.async.AsyncOperationListener;
import org.greenrobot.greendao.async.AsyncSession;
import org.greenrobot.greendao.query.Query;

import java.util.Collections;
import java.util.Date;
import java.util.List;


public class AlarmListAdapter extends BaseAdapter {
    private static String TAG = "adapter";

    private Activity mActivity;
    private LayoutInflater mInflater;
    private DaoSession mDaoSession;
    private List<Alarm> mAlarms;

    private TextView mTextNextAlarm;
    private String mNextAlarm;

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
        wActive.setChecked(alarm.getActive());
        wActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarm.setActive(isChecked);
                mDaoSession.insertOrReplace(alarm);
                ((MainActivity)mActivity).engageAlarm(alarm);
                if(isChecked) {
                    AlarmTimeFormatter.getNextAlarmTime(alarm.getNextAlarmTime(),true, mActivity);
                }
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
                                previewAlarm(alarmId);
                                return true;
                            case R.id.mi_delete:
                                deleteAlarm(alarmId);
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

    private void deleteAlarm(long alarmId) {
        // first deactivate it and disengage it
        Alarm alarm = mDaoSession.getAlarmDao().load(alarmId);
        alarm.setActive(false);
        ((MainActivity)mActivity).engageAlarm(alarm);
        // delete it
        mDaoSession.getAlarmDao().delete(alarm);
        updateAlarms();
    }

    private void previewAlarm(long alarmId) {
        Alarm alarm = mDaoSession.getAlarmDao().load(alarmId);
        Intent intent = AlarmReceiverActivity.newIntent(
                mActivity,
                alarmId,
                alarm.getRingtone(),
                alarm.getScheduleAlarmTime(mActivity).humanReadable,
                alarm.getVibrate(),
                MessageActivity.renderTaggedText(
                        alarm.getMessage().getText(),
                        mDaoSession.getTagDao(),
                        mActivity
                ).toString()
        );
        mActivity.startActivity(intent);
    }


    public void updateAlarms(){
        Query alarmQuery = mDaoSession.getAlarmDao().queryBuilder().build();
        AsyncSession asyncSession = mDaoSession.startAsyncSession();

        asyncSession.setListener(new AsyncOperationListener() {
            @Override
            public void onAsyncOperationCompleted(AsyncOperation operation) {
                List<Alarm>alarmsToSort = (List<Alarm>)operation.getResult();

                // get the next alarm that will go off
                Date nextAlarm = Collections.min(alarmsToSort,new AlarmNextTimeComparator()).getNextAlarmTime();
                mNextAlarm = "Next alarm is at " + AlarmTimeFormatter.convertTimeToReadable(nextAlarm,mActivity);

                // sort the alarms by scheduled time
                AlarmScheduleTimeComparator comparator = new AlarmScheduleTimeComparator();
                Collections.sort(alarmsToSort,comparator);
                mAlarms = alarmsToSort;

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                        mTextNextAlarm.setText(mNextAlarm);
                    }
                });

            }
        });
        asyncSession.queryList(alarmQuery);

    }
}
