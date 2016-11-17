package com.yugensoft.countdownalarm;

import java.util.Comparator;

/**
 * Function to return a Comparator used for sorting alarms by next alarm time
 */
public class AlarmNextTimeComparator implements Comparator<Alarm> {
    @Override
    public int compare(Alarm o1, Alarm o2) {
        long time1 = o1.getNextAlarmTime().getTime();
        long time2 = o2.getNextAlarmTime().getTime();
        return Long.valueOf(time1).compareTo(time2);
    }
}
