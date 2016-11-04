package com.yugensoft.countdownalarm;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.greenrobot.greendao.database.Database;

public class CountdownAlarmApplication extends Application {
    private Tracker mTracker;
    private DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();

        // todo says should only be used in development, because drops tables on upgrade, ???
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "countdown-alarm-db");
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
