package com.yugensoft.countdownalarm;

import java.util.Comparator;

/**
 * Function to return a Comparator used for sorting alarms by time
 */
public class AlarmComparator implements Comparator<Alarm> {
    @Override
    public int compare(Alarm o1, Alarm o2) {
        String[] cronParts1 = o1.getSchedule().split("\\s+");
        int hours1 = Integer.valueOf(cronParts1[Alarm.CRON_EXPRESSION_HOURS]);
        int minutes1 = Integer.valueOf(cronParts1[Alarm.CRON_EXPRESSION_MINUTES]);
        String[] cronParts2 = o2.getSchedule().split("\\s+");
        int hours2 = Integer.valueOf(cronParts2[Alarm.CRON_EXPRESSION_HOURS]);
        int minutes2 = Integer.valueOf(cronParts2[Alarm.CRON_EXPRESSION_MINUTES]);
        // this should be enough to compare them
        int compare1 = hours1 * 100 + minutes1;
        int compare2 = hours2 * 100 + minutes2;
        return Integer.valueOf(compare1).compareTo(compare2);
    }
}
