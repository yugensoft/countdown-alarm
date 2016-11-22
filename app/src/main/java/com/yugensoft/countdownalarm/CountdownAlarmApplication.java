package com.yugensoft.countdownalarm;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.greenrobot.greendao.database.Database;

public class CountdownAlarmApplication extends Application {
    private Tracker mTracker;
    private DaoSession daoSession;

    // Application flavour (for different 'levels' of the application, i.e. free or paid)
    public enum e_ApplicationFlavour {
        FREE(0),
        PREMIUM(1);

        private int value;
        private e_ApplicationFlavour(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }
    }
    private e_ApplicationFlavour mApplicationFlavour;

    @Override
    public void onCreate() {
        super.onCreate();

        mApplicationFlavour = e_ApplicationFlavour.FREE;

        DaoMaster.OpenHelper helper = new CountdownAlarmDbOpenHelper(this, "countdown-alarm-db");
        Database db = helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }
}
