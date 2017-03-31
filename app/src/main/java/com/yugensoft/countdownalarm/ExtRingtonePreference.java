package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;


/**
 * Class that extends a RingtonePreference for the purpose of interfacing it with a database
 */
public class ExtRingtonePreference extends RingtonePreference {
    private String mInitialRingtone;

    public ExtRingtonePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExtRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtRingtonePreference(Context context) {
        super(context);
    }

    @Override
    protected Uri onRestoreRingtone() {
        if(mInitialRingtone == null || mInitialRingtone.equals("")) {
            return null;
        } else {
            return Uri.parse(mInitialRingtone);
        }
    }

    public void setInitialRingtone(String initialRingtone) {
        this.mInitialRingtone = initialRingtone;
    }

}
