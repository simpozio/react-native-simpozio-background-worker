package com.simpozio.android.background.heartbeat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.http.AsyncHttpAgent;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Request;
import okhttp3.RequestBody;

import static com.simpozio.android.background.SimpozioJavaService.HEADERS_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioJavaService.REQUEST_BODY_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioJavaService.SIMPOZIO_URL_EXTRA;

public final class HeartbeatHttpAgent extends AsyncHttpAgent {

    private static final String LOG_TAG = "HeartbeatHttpAgent";

    private static final PeriodFormatter PERIOD_FORMATTER = createPeriodFormatter();

    public HeartbeatHttpAgent(EventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public Request prepareRequest() throws JSONException {

        Log.d(LOG_TAG, "prepareRequest started");

        Bundle requestBody = this.requestBody.get();
        String simpozioUrl = this.url.get();
        Bundle headers = this.headers.get();

        if (headers == null || requestBody == null) {
            throw illegalState("data is null");
        } else if (simpozioUrl == null) {
            throw illegalState("url is null");
        }

        this.eventLoopPeriodMs = nextHeartbeatPeriod(requestBody);

        Request.Builder requestBuilder = new Request.Builder();

        for (String key : headers.keySet()) {
            requestBuilder.header(key, headers.getString(key));
        }

        try {
            return requestBuilder
                    .url(simpozioUrl)
                    .header("Date", DateFormatted.now().date())
                    .post(RequestBody.create(MEDIA_TYPE, prepareRequestBodyContent(requestBody)))
                    .build();
        } finally {
            Log.d(LOG_TAG, "prepareRequest finished");
        }
    }

    @Override
    public void notify(Intent event) {
        Log.d(LOG_TAG, "notify started");
        this.requestBody.set(event.getBundleExtra(REQUEST_BODY_EVENT_BUNDLE));
        this.headers.set(event.getBundleExtra(HEADERS_EVENT_BUNDLE));
        this.url.set(event.getStringExtra(SIMPOZIO_URL_EXTRA));
        Log.d(LOG_TAG, "notify finished");
    }

    private static long nextHeartbeatPeriod(Bundle requestBody) {
        return (long) PERIOD_FORMATTER.parsePeriod(requestBody.getString("next").trim()).getMillis();
    }

    private static String prepareRequestBodyContent(Bundle metadata) throws JSONException {
        try {
            Log.d(LOG_TAG, "prepareRequestBodyContent started");
            if (metadata.containsKey("touchpoint") && metadata.containsKey("state") && metadata.containsKey("timestamp")) {
                JSONObject content = new JSONObject();
                for (String key : metadata.keySet()) {
                    content.put(key, metadata.getString(key));
                }
                return content.toString();
            } else {
                throw illegalState("touchpoint, state, timestamp are required fields");
            }
        } finally {
            Log.d(LOG_TAG, "prepareRequestBodyContent finished");
        }
    }

    private static PeriodFormatter createPeriodFormatter() {
        return new PeriodFormatterBuilder()
                .appendDays()
                .appendSuffix("d")
                .appendHours()
                .appendSuffix("h")
                .appendMinutes()
                .appendSuffix("m")
                .toFormatter();
    }
}
