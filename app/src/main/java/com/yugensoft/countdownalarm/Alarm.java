package com.yugensoft.countdownalarm;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.DaoException;

@Entity
public class Alarm {
    // Schema area
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    private String schedule; // cron schedule string
    // nullable
    private int repeats; // how many more times to trigger this alarm before de-activating it; forever if null
    @NotNull
    private boolean active; // whether alarm is active or not
    // todo: ringtone type?
    @NotNull
    private boolean vibrate; // vibrate on/off
    private String label; // user-set name of the alarm
    @ToOne @NotNull
    private Message message;



    
    // Generated area -----------------

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1493767907)
    private transient AlarmDao myDao;

    @Generated(hash = 1103821362)
    private transient boolean message__refreshed;


    // Generated & setter-getter area

    @Generated(hash = 2065598881)
    public Alarm(Long id, @NotNull String schedule, int repeats, boolean active, boolean vibrate, String label) {
        this.id = id;
        this.schedule = schedule;
        this.repeats = repeats;
        this.active = active;
        this.vibrate = vibrate;
        this.label = label;
    }

    @Generated(hash = 1972324134)
    public Alarm() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchedule() {
        return this.schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public boolean getActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getVibrate() {
        return this.vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 955954570)
    public Message getMessage() {
        if (message != null || !message__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            MessageDao targetDao = daoSession.getMessageDao();
            targetDao.refresh(message);
            message__refreshed = true;
        }
        return message;
    }

    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 93566467)
    public Message peakMessage() {
        return message;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1971499210)
    public void setMessage(@NotNull Message message) {
        if (message == null) {
            throw new DaoException(
                    "To-one property 'message' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.message = message;
            message__refreshed = true;
        }
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    public int getRepeats() {
        return this.repeats;
    }

    public void setRepeats(int repeats) {
        this.repeats = repeats;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 145394493)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getAlarmDao() : null;
    }


}
