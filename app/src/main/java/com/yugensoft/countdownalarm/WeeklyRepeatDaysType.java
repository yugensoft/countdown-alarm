package com.yugensoft.countdownalarm;

import java.util.Set;

/**
 * Class used to return Weekly repeat days in different formats
 */
public class WeeklyRepeatDaysType {
    public final Set<String> fullWords;
    public final String humanReadable;

    public WeeklyRepeatDaysType(Set<String> fullWords, String humanReadable){
        this.fullWords = fullWords;
        this.humanReadable = humanReadable;
    }
}