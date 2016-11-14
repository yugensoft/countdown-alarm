package com.yugensoft.countdownalarm;

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


}
