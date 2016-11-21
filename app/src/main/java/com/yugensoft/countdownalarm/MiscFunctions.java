package com.yugensoft.countdownalarm;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MiscFunctions {
    /**
     * Returns the correct suffix for the last digit (1st, 2nd, .. , 13th, .. , 23rd)
     */
    public static String getLastDigitSufix(int number) {
        switch( (number<20) ? number : number%10 ) {
            case 1 : return "st";
            case 2 : return "nd";
            case 3 : return "rd";
            default : return "th";
        }
    }

    /**
     * Function to load an ad request into an adview
     * @param adView Which adView to load it into
     */
    public static void loadAdIntoAdView(AdView adView){
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("F961E2E362704F9592CC2F9CC025A1BF")
                .addKeyword("alarm")
                .addKeyword("speaking")
                .addKeyword("count-down")
                .addKeyword("countdown")
                .addKeyword("motivation")
                .addKeyword("encouragement")
                .addKeyword("insomnia")
                .addKeyword("well-rested")
                .addKeyword("lethargic")
                .addKeyword("bedtime")
                .addKeyword("exhausted")
                .addKeyword("exhaustion")
                .addKeyword("bed")
                .build();
        adView.loadAd(adRequest);
    }
}
