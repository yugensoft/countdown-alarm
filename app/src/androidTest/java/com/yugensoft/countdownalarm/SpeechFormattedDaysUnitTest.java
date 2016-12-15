package com.yugensoft.countdownalarm;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.joda.time.Days;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Tests that the TimeFormatters.convertDaysToSpeechFormat() function is working as expected.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SpeechFormattedDaysUnitTest {
    private static final String TAG = "unit-test";

    @Test
    public void formatting_isCorrect() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        String speechFormat = "MM dd";

        for(int x=0; x<=365; x++){
            String s = TimeFormatters.convertDaysToSpeechFormat(context.getResources(), Days.days(x),speechFormat,false);
            Log.d(TAG, s + "\t\t" + String.valueOf(x));
        }
//        assertEquals("1 week 3 days",s);
    }
}