package com.yugensoft.countdownalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// todo: remember to enable this if there are active alarms
public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the alarm here.
        }
    }
}
