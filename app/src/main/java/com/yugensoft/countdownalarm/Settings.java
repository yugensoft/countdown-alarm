package com.yugensoft.countdownalarm;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class Settings {
    // Schema area
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    private long snoozeDuration;
    @NotNull
    private int alarmVolume;




    // Generated area -----------------


    @Generated(hash = 1308404)
    public Settings(Long id, long snoozeDuration, int alarmVolume) {
        this.id = id;
        this.snoozeDuration = snoozeDuration;
        this.alarmVolume = alarmVolume;
    }
    @Generated(hash = 456090543)
    public Settings() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public long getSnoozeDuration() {
        return this.snoozeDuration;
    }
    public void setSnoozeDuration(long snoozeDuration) {
        this.snoozeDuration = snoozeDuration;
    }
    public int getAlarmVolume() {
        return this.alarmVolume;
    }
    public void setAlarmVolume(int alarmVolume) {
        this.alarmVolume = alarmVolume;
    }
}
