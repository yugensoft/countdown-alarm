package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.greenrobot.greendao.database.Database;

import java.util.List;

import static com.yugensoft.countdownalarm.AlarmFunctions.getAllAlarmsSorted;
import static com.yugensoft.countdownalarm.AlarmFunctions.getNextAlarmTimeReadable;
import static com.yugensoft.countdownalarm.AlarmFunctions.setNotification;

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            DaoMaster.OpenHelper helper = new CountdownAlarmDbOpenHelper(context, "countdown-alarm-db");
            Database db = helper.getWritableDb();
            DaoSession daoSession = new DaoMaster(db).newSession();

            AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            AlarmFunctions.engageAllAlarms(context,daoSession,alarmManager);

            updateNextAlarmSystemNotification(context, daoSession);
        }
    }

    /**
     * Update the system notification of next alarm
     */
    private void updateNextAlarmSystemNotification(final Context context, final DaoSession daoSession){
        AlarmFunctions.GetAlarmsCallback callback = new AlarmFunctions.GetAlarmsCallback() {
            @Override
            public void callback(final List<Alarm> alarms) {
                // update notification
                String nextAlarmTime = getNextAlarmTimeReadable(alarms, context);
                setNotification(nextAlarmTime,context,context.getString(R.string.app_name));
            }
        };

        getAllAlarmsSorted(daoSession,callback);

    }
}
