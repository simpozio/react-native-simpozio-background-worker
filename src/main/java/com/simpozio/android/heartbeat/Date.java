package com.simpozio.android.heartbeat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class Date {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static String now() {
        return DATE_FORMAT.format(Calendar.getInstance());
    }
}
