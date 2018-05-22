package com.simpozio.android.background.heartbeat;

import android.os.Bundle;

import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.http.AsyncHttpAgent;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Request;
import okhttp3.RequestBody;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class HeartbeatHttpAgent extends AsyncHttpAgent {

    private static PeriodFormatter PERIOD_FORMATTER = getPeriodFormatter();


    public HeartbeatHttpAgent(EventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public Request prepareRequest() throws JSONException {

        Bundle requestBody = this.requestBody.get();
        String simpozioUrl = this.url.get();
        Bundle headers = this.headers.get();

        if (headers == null || requestBody == null) {
            throw illegalState("data is null");
        } else if (simpozioUrl == null) {
            throw illegalState("url is null");
        }

        this.next = nextHeartbeatPeriod(requestBody);

        Request.Builder requestBuilder = new Request.Builder();

        for (String key : headers.keySet()) {
            requestBuilder.header(key, headers.getString(key));
        }

        DateFormatted now = DateFormatted.now();

        return requestBuilder
                .url(simpozioUrl)
                .header("Date", now.date())
                .post(RequestBody.create(MEDIA_TYPE, prepareRequestBodyContent(requestBody, now)))
                .build();
    }

    private long nextHeartbeatPeriod(Bundle requestBody) {

        String next = requestBody.getString("next");

        if (next != null) {
            Period nextPeriod = PERIOD_FORMATTER.parsePeriod(next);
            return DAYS.toMillis(nextPeriod.getDays()) +
                    HOURS.toMillis(nextPeriod.getHours()) +
                    MINUTES.toMillis(nextPeriod.getMinutes()) +
                    SECONDS.toMillis(nextPeriod.getSeconds()) +
                    MILLISECONDS.toMillis(nextPeriod.getMillis());
        }
        return this.next; // return previous value
    }

    private static String prepareRequestBodyContent(Bundle metadata, DateFormatted now) throws JSONException {
        if (metadata.containsKey("touchpoint") && metadata.containsKey("state")) {
            JSONObject content = new JSONObject();
            for (String key : metadata.keySet()) {
                content.put(key, metadata.getString(key));
            }
            content.put("timestamp", now.timestamp());
            return content.toString();
        } else {
            throw illegalState("touchpoint and state are required fields");
        }
    }

    private static PeriodFormatter getPeriodFormatter() {
        return new PeriodFormatterBuilder()
                .appendDays().appendSuffix("d")
                .appendHours().appendSuffix("h")
                .appendMinutes().appendSuffix("m")
                .appendSeconds().appendSuffix("s")
                .appendMillis().appendSuffix("ms")
                .toFormatter();
    }

}
