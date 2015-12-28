package com.ssl.cltracker.data;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Utils {

    public static void say(Object s) {
        System.out.println(s);
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     * @param branches an array objects to be joined. Strings will be formed from
     *     the objects by calling object.toString().
     */
    public static <K> String join(CharSequence delimiter, List<K> branches, boolean quote) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (K token : branches) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            if (quote)
                sb.append("'");
            sb.append(token);
            if (quote)
                sb.append("'");
        }
        return sb.toString();
    }

    public static String getDateInString() {
        Calendar cal = Calendar.getInstance();
        DecimalFormat df = new DecimalFormat("00", DecimalFormatSymbols.getInstance(Locale.US));
        String month = df.format(cal.get(Calendar.MONTH) + 1);
        String day = df.format(cal.get(Calendar.DAY_OF_MONTH));
        String hour = df.format(cal.get(Calendar.HOUR_OF_DAY));
        String min = df.format(cal.get(Calendar.MINUTE));
        return cal.get(Calendar.YEAR) + "/" + month + "/" + day + " at " + hour + ":" + min;
    }
}
