package com.neusoft.hospital.core.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateExt {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private static final SimpleDateFormat WEEK_DAY_FMT = new SimpleDateFormat("EEEE", Locale.CHINA);

    private DateExt() {}

    public static String today() {
        return DATE_FMT.format(new Date());
    }

    public static String weekDay(String date) {
        try {
            return WEEK_DAY_FMT.format(DATE_FMT.parse(date));
        } catch (Exception e) {
            return "";
        }
    }

    public static String addDays(String date, int days) {
        try {
            Date d = DATE_FMT.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.add(Calendar.DAY_OF_MONTH, days);
            return DATE_FMT.format(cal.getTime());
        } catch (Exception e) {
            return date;
        }
    }
}