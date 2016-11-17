package com.yugensoft.countdownalarm;

import android.content.Context;
import android.media.RingtoneManager;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.DaoException;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Entity
public class Alarm {
    // Schema area
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    private String schedule; // cron schedule string
    // nullable
    private Integer repeats; // how many more times to trigger this alarm before de-activating it; forever if null
    @NotNull
    private boolean active; // whether alarm is active or not
    private String ringtone; // the ringtone (uri string)
    @NotNull
    private boolean vibrate; // vibrate on/off
    private String label; // user-set name of the alarm

    private Long messageId;
    @ToOne(joinProperty = "messageId")
    private Message message;

    public static Alarm newDefaultAlarm(){
        String ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
        return new Alarm(null, "0 0 8 ? * * *", 1, true, ringtone, true, "", null);
    }

    // Constants related to the schedule string
    public static final int CRON_EXPRESSION_SECONDS = 0;
    public static final int CRON_EXPRESSION_MINUTES = 1;
    public static final int CRON_EXPRESSION_HOURS = 2;
    public static final int CRON_EXPRESSION_DAYS_OF_MONTH = 3;
    public static final int CRON_EXPRESSION_MONTHS = 4;
    public static final int CRON_EXPRESSION_DAYS_OF_WEEK = 5;
    public static final int CRON_EXPRESSION_YEARS = 6;

    /**
     * Function to set a standard "hh:mm" + Mon,Tue...Sun repeating alarm / no-repeat
     * Takes first three characters of each element in repeatDays to give the day
     * @param hour Alarm hour
     * @param minute Alarm minute
     * @param repeatDays A set of title-case days
     */
    public void setSchedule(int hour, int minute, Set<String> repeatDays){
        StringBuilder daysOfWeekExpr = new StringBuilder();
        String delim = "";
        if(repeatDays.size() == 0){
            // non-repeated alarm
            repeats = 1;
            daysOfWeekExpr.append("*");
        } else {
            // repeat on the given days
            repeats = null;
            SimpleDateFormat sdfShort = new SimpleDateFormat("E", Locale.getDefault());
            SimpleDateFormat sdfLong = new SimpleDateFormat("EEEE", Locale.getDefault());

            for (String sLong : repeatDays){
                try {
                    String sShort = sdfShort.format(sdfLong.parse(sLong));
                    daysOfWeekExpr.append(delim).append(sShort);
                } catch (ParseException e){
                    throw new RuntimeException("Error parsing cron-style days-of-week to long-style");
                }
                delim = ",";
            }
        }

        // Quartz scheduler format: s m H DoM M DoW y
        String cronStr = String.format("0 %s %s ? * %s *", minute, hour, daysOfWeekExpr.toString());

        schedule = cronStr;
    }

    /**
     * Comparator function used to sort days by weekly order
     */
    private static class DayComparator implements Comparator<String>{
        @Override
        public int compare(String o1, String o2) {
            // get day numbers
            int day1, day2;
            day1 = getDayOfWeekNumber(o1);
            day2 = getDayOfWeekNumber(o2);

            return Integer.valueOf(day1).compareTo(day2);
        }

        private int getDayOfWeekNumber(String day){
            if(day.length()<3) {
                throw new IllegalArgumentException("Not a valid day");
            }
            String daySelector = day.substring(0,3).toLowerCase();
            if(daySelector.equals("mon")) {
                return 1;
            } else if(daySelector.equals("tue")) {
                return 2;
            } else if(daySelector.equals("wed")) {
                return 3;
            } else if(daySelector.equals("thu")) {
                return 4;
            } else if(daySelector.equals("fri")) {
                return 5;
            } else if(daySelector.equals("sat")) {
                return 6;
            } else if(daySelector.equals("sun")) {
                return 7;
            } else {
                throw new IllegalArgumentException("Invalid day string passed");
            }
        }
    }
    /**
     * @return The schedule in a human-readable string
     */
    public WeeklyRepeatDaysType getScheduleRepeatDays(){
        String[] cronParts = schedule.split("\\s+");
        if(cronParts.length != 7){
            throw new RuntimeException("Attempt to render from malformed schedule string: " + schedule);
        }

        String daysOfWeek = cronParts[CRON_EXPRESSION_DAYS_OF_WEEK];
        if(repeats == null && !daysOfWeek.equals("*")) { // a ever-repeating days-list schedule
            StringBuilder outputHumanReadable = new StringBuilder("Every ");
            Set<String> outputFullWords = new HashSet<String>();

            SimpleDateFormat sdfShort = new SimpleDateFormat("E", Locale.getDefault());
            SimpleDateFormat sdfLong = new SimpleDateFormat("EEEE", Locale.getDefault());

            String token = "";
            String[] daysList = daysOfWeek.split(",");

            // Check integrity
            if(daysList.length == 0 || daysList.length > 7){
                throw new RuntimeException("Corrupted list of days: " + daysOfWeek);
            }

            try {
                if(daysList.length == 1) {
                    // Use full day name if only one day
                    outputHumanReadable.append(sdfLong.format(sdfShort.parse(daysList[0])));
                } else if(daysList.length == 7) {
                    // Just say 'day' if all days
                    outputHumanReadable.append("day");
                } else {
                    // Sort days in weekly order
                    Arrays.sort(daysList,new DayComparator());
                    // Use short day names
                    for (String day : daysList) {
                        outputHumanReadable.append(token).append(day);
                        outputFullWords.add(sdfLong.format(sdfShort.parse(day)));

                        token = ", ";
                    }
                }
            } catch (ParseException e){
                throw new RuntimeException("Error parsing cron-style days-of-week to long-style");
            }

            return new WeeklyRepeatDaysType(outputFullWords,outputHumanReadable.toString());

        } else if(repeats <= 1 && daysOfWeek.equals("*")){ // a single-repeating alarm
                return new WeeklyRepeatDaysType(new HashSet<String>(),"Never");
        } else {
            throw new RuntimeException("Unexpected alarm schedule/repeats: " + String.valueOf(schedule) + " / " + String.valueOf(repeats));
        }
    }

    /**
     * @return The alarm time in human-readable format
     */
    public AlarmTimeType getScheduleAlarmTime(Context context){
        String[] cronParts = schedule.split("\\s+");
        if(cronParts.length != 7){
            throw new RuntimeException("Attempt to get time from malformed schedule string: "+schedule);
        }

        int hour = Integer.valueOf(cronParts[CRON_EXPRESSION_HOURS]);
        int minute = Integer.valueOf(cronParts[CRON_EXPRESSION_MINUTES]);

        String time = AlarmTimeFormatter.convertTimeToReadable(hour,minute,context);
        return new AlarmTimeType(hour, minute, time);
    }

    /**
     * @return The next Alarm time
     */
    public Date getNextAlarmTime(){
        try {
            CronExpression cronExpression = new CronExpression(schedule);
            Date now = new Date();
            return cronExpression.getNextValidTimeAfter(now);
        } catch (ParseException e){
            throw new RuntimeException("Attempt to get Next Alarm Time from invalid schedule: "+schedule);
        }
    }

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1493767907)
    private transient AlarmDao myDao;
    @Generated(hash = 1313305291)
    public Alarm(Long id, @NotNull String schedule, Integer repeats, boolean active, String ringtone, boolean vibrate, String label,
            Long messageId) {
        this.id = id;
        this.schedule = schedule;
        this.repeats = repeats;
        this.active = active;
        this.ringtone = ringtone;
        this.vibrate = vibrate;
        this.label = label;
        this.messageId = messageId;
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
    @Generated(hash = 1728529602)
    private transient Long message__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1905538284)
    public Message getMessage() {
        Long __key = this.messageId;
        if (message__resolvedKey == null || !message__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            MessageDao targetDao = daoSession.getMessageDao();
            Message messageNew = targetDao.load(__key);
            synchronized (this) {
                message = messageNew;
                message__resolvedKey = __key;
            }
        }
        return message;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 770507318)
    public void setMessage(Message message) {
        synchronized (this) {
            this.message = message;
            messageId = message == null ? null : message.getId();
            message__resolvedKey = messageId;
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
    public String getRingtone() {
        return this.ringtone;
    }
    public void setRingtone(String ringtone) {
        this.ringtone = ringtone;
    }
    public void setRepeats(Integer repeats) {
        this.repeats = repeats;
    }
    public Integer getRepeats() {
        return this.repeats;
    }

    public Long getMessageId() {
        return this.messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 145394493)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getAlarmDao() : null;
    }


}
