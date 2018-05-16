package com.simpozio.android.background.heartbeat;

import android.os.Bundle;

import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.http.AsyncHttpAgent;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Request;
import okhttp3.RequestBody;

public final class HeartbeatHttpAgent extends AsyncHttpAgent {

    private static final PeriodFormatter PERIOD_FORMATTER = createPeriodFormatter();

    public HeartbeatHttpAgent(EventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public Request prepareRequest() throws JSONException {

        Bundle headers = this.headers.get();
        Bundle requestBody = this.requestBody.get();

        if (headers == null || requestBody == null) {
            throw new IllegalStateException("data is null");
        }

        this.eventLoopPeriodMs = nextHeartbeatPeriod(requestBody);

        Request.Builder requestBuilder = new Request.Builder();

        for (String key : headers.keySet()) {
            requestBuilder.header(key, headers.getString(key));
        }

        requestBuilder.header("Date", DateFormatted.now().date());

        return requestBuilder
                .url(simpozioAddress.get() + url.get())
                .post(RequestBody.create(MEDIA_TYPE, prepareRequestBodyContent(requestBody)))
                .build();
    }

    private static long nextHeartbeatPeriod(Bundle requestBody) {
        return (long) PERIOD_FORMATTER.parsePeriod(requestBody.getString("next").trim()).getMillis();
    }

    private static String prepareRequestBodyContent(Bundle metadata) throws JSONException {
        if (metadata.containsKey("touchpoint") && metadata.containsKey("state") && metadata.containsKey("timestamp")) {
            JSONObject content = new JSONObject();
            for (String key : metadata.keySet()) {
                content.put(key, metadata.getString(key));
            }
            return content.toString();
        } else {
            throw illegalArgument("touchpoint, state, timestamp are required fields");
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
