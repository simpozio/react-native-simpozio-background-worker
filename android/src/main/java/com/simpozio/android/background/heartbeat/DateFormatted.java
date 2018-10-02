package com.simpozio.android.background.heartbeat;

import android.annotation.SuppressLint;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public abstract class DateFormatted {

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public abstract String timestamp();
    public abstract String date();

    public static DateFormatted parse(final String raw, final long plus) {
        return new DateFormatted() {
            @Override
            public String timestamp() {
                return DateTime.parse(raw).plus(plus).toString();
            }

            @Override
            public String date() {
                try {
                    return DATE_FORMAT.parse(raw).toString();
                } catch (ParseException e) {
                    return now().date();
                }
            }
        };
    }

    public static DateFormatted now() {
        return new DateFormatted() {

            Date now = new Date();

            @Override
            public String timestamp() {
                return TIMESTAMP_FORMAT.format(now);
            }

            @Override
            public String date() {
                return DATE_FORMAT.format(now);
            }
        };
    }
}
