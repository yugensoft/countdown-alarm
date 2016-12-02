package com.yugensoft.countdownalarm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class CountdownAlarmDbOpenHelper extends DaoMaster.OpenHelper {
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
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle changes in the database between versions
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion)
        {
            switch (upgradeTo)
            {
                // todo, fix / remove this, throw exception on default, add case by case
                default:
                    DaoMaster.dropAllTables(getWritableDb(),true);
                    onCreate(db);
                    break;

            }
            upgradeTo++;
        }

    }
}
