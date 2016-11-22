package com.yugensoft.countdownalarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.greenrobot.greendao.database.Database;

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            DaoMaster.OpenHelper helper = new CountdownAlarmDbOpenHelper(context, "countdown-alarm-db");
            Database db = helper.getWritableDb();
            DaoSession daoSession = new DaoMaster(db).newSession();

            AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            AlarmFunctions.engageAllAlarms(context,daoSession,alarmManager);
        }
    }
}
