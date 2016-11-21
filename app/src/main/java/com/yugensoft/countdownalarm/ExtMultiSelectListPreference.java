package com.yugensoft.countdownalarm;

import android.content.Context;
import android.os.Parcelable;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

/**
 * A none-crashing version of MultiSelectListPreference
 * Fixing google's sloppy work
 */

public class ExtMultiSelectListPreference extends MultiSelectListPreference {
    public ExtMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtMultiSelectListPreference(Context context) {
        super(context);
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        try {
            super.onRestoreInstanceState(state);
        } catch (IllegalArgumentException e) {
            // Fix of crash https://code.google.com/p/android/issues/detail?id=70088
            if (!isPersistent())
                super.onRestoreInstanceState(((BaseSavedState) state).getSuperState());
            else
                throw e;
        }
    }

}
