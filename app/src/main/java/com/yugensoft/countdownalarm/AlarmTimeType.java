package com.yugensoft.countdownalarm;

public class AlarmTimeType {
    public final int hour;
    public final int minute;
    public final String humanReadable;

    public AlarmTimeType(int hour, int minute, String humanReadable){
        this.hour = hour;
        this.minute = minute;
        this.humanReadable = humanReadable;
    }
}
