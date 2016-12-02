package com.yugensoft.countdownalarm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.greenrobot.greendao.database.Database;

public class CountdownAlarmDbOpenHelper extends DaoMaster.OpenHelper {

    /**
     * for Development purposes
     * allows unaccounted for schema numbers to cause all tables to be dropped
     * should be turned off before deployment
     */
    private static final boolean DEV = false;

    public CountdownAlarmDbOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }
    public CountdownAlarmDbOpenHelper(Context context, String name) {
        super(context, name);
    }

    /**
     * Performs an iterative upgrade of the database tables
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        // Handle changes in the database between versions
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion)
        {
            switch (upgradeTo)
            {
                case 9:
                    // do nothing, no schema changes
                    break;

                default:
                    if(DEV) {
                        DaoMaster.dropAllTables(db, true);
                        onCreate(db);
                    } else {
                        throw new RuntimeException("Unknown schema change");
                    }
                    break;

            }
            upgradeTo++;
        }

    }
}
