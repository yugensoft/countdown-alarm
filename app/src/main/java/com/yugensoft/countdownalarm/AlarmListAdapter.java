package com.yugensoft.countdownalarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class AlarmListAdapter extends BaseAdapter {
    private static String TAG = "adapter";

    private Activity mActivity;
    private LayoutInflater mInflater;
    private DaoSession mDaoSession;
    private List<Alarm> mAlarms;

    public AlarmListAdapter(Activity activity, DaoSession daoSession) {
        mActivity = activity;
        mDaoSession = daoSession;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        // views
        CheckBox wActive = (CheckBox)convertView.findViewById(R.id.checkbox_active);
        TextView wAlarmTime = (TextView)convertView.findViewById(R.id.text_alarm_time);
        TextView wAlarmSchedule = (TextView)convertView.findViewById(R.id.text_alarm_schedule);
        TextView wAlarmLabel = (TextView)convertView.findViewById(R.id.text_alarm_label);
        ImageView wMessage = (ImageView)convertView.findViewById(R.id.iv_message);
        ImageButton wMenu = (ImageButton)convertView.findViewById(R.id.ib_menu);
        // layouts
        LinearLayout llAlarmSchedule= (LinearLayout)convertView.findViewById(R.id.ll_alarm_schedule);
        LinearLayout llAlarmLabel = (LinearLayout)convertView.findViewById(R.id.ll_alarm_label);
        // set on-clicks
        wActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarm.setActive(isChecked);
                mDaoSession.insertOrReplace(alarm);
                ((MainActivity)mActivity).engageAlarm(alarm);
                if(isChecked) {
                    AlarmTimeFormatter.getNextAlarmTime(alarm.getNextAlarmTime(), mActivity);
                }
            }
        });

        // populate data
        wActive.setChecked(alarm.getActive());
        wAlarmTime.setText(alarm.getScheduleAlarmTime(mActivity).humanReadable);

        String scheduleText = alarm.getScheduleRepeatDays().humanReadable;
        scheduleText = scheduleText.equals("Never") ? "" : scheduleText;
        wAlarmSchedule.setText(scheduleText);

        wAlarmLabel.setText(alarm.getLabel());
        if(alarm.getMessageId() == null) {
            wMessage.setVisibility(View.INVISIBLE);
        }else{
            wMessage.setVisibility(View.VISIBLE);
        }

        return convertView;
    }


    public void updateAlarms(){
        Query alarmQuery = mDaoSession.getAlarmDao().queryBuilder().build();
        AsyncSession asyncSession = mDaoSession.startAsyncSession();

        asyncSession.setListener(new AsyncOperationListener() {
            @Override
            public void onAsyncOperationCompleted(AsyncOperation operation) {
                List<Alarm>alarmsToSort = (List<Alarm>)operation.getResult();
                AlarmComparator comparator = new AlarmComparator();
                Collections.sort(alarmsToSort,comparator);
                mAlarms = alarmsToSort;
            }
        });
        asyncSession.queryList(alarmQuery);
        notifyDataSetChanged();
    }
}
