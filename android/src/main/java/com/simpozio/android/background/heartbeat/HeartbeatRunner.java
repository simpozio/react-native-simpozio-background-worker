package com.simpozio.android.background.heartbeat;

import android.os.Bundle;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HeartbeatRunner extends Thread implements EventPublisher {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static final PeriodFormatter PERIOD_FORMATTER = createPeriodFormatter();

    public final AtomicReference<Bundle> requestBody = new AtomicReference<>(null);
    public final AtomicReference<String> simpozioAddress = new AtomicReference<>(null);
    public final AtomicReference<Bundle> headers = new AtomicReference<>(null);
    public final AtomicReference<String> url = new AtomicReference<>(null);

    private final EventPublisher eventPublisher;

    private long nextHeartbeatPeriodMs = 5000L;
    private boolean failed = false;

    public HeartbeatRunner(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void start() {
        try {
            super.start();
        } catch (IllegalThreadStateException ignored) {
            this.fireEvent(Events.startFailed(illegalState("unexpected state on start: " + this.getState())));
        }
    }

    @Override
    public void run() {

        long startupPoint = this.started();

        long lastFailed = 0;

        OkHttpClient httpClient = new OkHttpClient();

        while (!isInterrupted()) {
            try {
                try {
                    Response response = httpClient.newCall(prepareRequest()).execute();
                    if (!response.isSuccessful()) {
                        this.onUnsuccessfullyResponse(response.code(), response.message());
                        lastFailed = System.currentTimeMillis();
                    } else {
                        response.close();
                        this.onSuccess(lastFailed);
                    }
                } catch (Exception cause) {
                    this.onException(cause);
                    lastFailed = System.currentTimeMillis();
                }
                Thread.sleep(this.nextHeartbeatPeriodMs);
            } catch (InterruptedException ignored) {
                this.interrupt(); // set flag
            }
        }
        this.finish(System.currentTimeMillis() - startupPoint);
    }

    @Override
    public void interrupt() {
        if (isInterrupted()) {
            this.fireEvent(Events.stopFailed(illegalState("already interrupted")));
        } else if (!isAlive()) {
            this.fireEvent(Events.stopFailed(illegalState("HeartbeatRunner died")));
        } else {
            super.interrupt();
        }
    }

    @Override
    public void fireEvent(Bundle event) {
        this.eventPublisher.fireEvent(event);
    }

    private long started() {
        this.fireEvent(Events.started());
        return System.currentTimeMillis();
    }

    private void finish(long uptime) {
        this.fireEvent(Events.stopped(uptime));
    }

    private void onSuccess(long lastFailed) {
        if (this.failed) {
            this.fireEvent(Events.resume(System.currentTimeMillis() - lastFailed));
            this.failed = false;
        }
    }

    private void onException(Exception cause) {
        this.fireEvent(Events.exception(cause));
    }

    private void onUnsuccessfullyResponse(int code, String message) {
        if (!this.failed) {
            this.fireEvent(Events.unsuccessfullyResponse(code, message));
            this.failed = true;
        }
    }

    private Request prepareRequest() throws JSONException {

        // DIRTY READING ON UPDATE IS POSSIBLE!

        Bundle headers = this.headers.get();
        Bundle requestBody = this.requestBody.get();

        if (headers == null || requestBody == null) {
            throw new IllegalStateException("data is null");
        }

        this.nextHeartbeatPeriodMs = this.acceptHeartbeatPeriod(requestBody);

        Request.Builder requestBuilder = new Request.Builder();

        for (String key : headers.keySet()) {
            requestBuilder.header(key, headers.getString(key));
        }

        DateFormatted now = DateFormatted.now();

        requestBuilder.header("Date", now.date());

        return requestBuilder
                .url(simpozioAddress.get() + url.get())
                .post(RequestBody.create(MEDIA_TYPE, prepareRequestBodyContent(requestBody)))
                .build();
    }

    private long acceptHeartbeatPeriod(Bundle metadata) {
        return (long) PERIOD_FORMATTER.parsePeriod(metadata.getString("next").trim()).getMillis();
    }

    private static String prepareRequestBodyContent(Bundle metadata) throws JSONException {
        if (metadata.containsKey("touchpoint") && metadata.containsKey("state") && metadata.containsKey("timestamp")) {
            throw illegalArgument("touchpoint, state, timestamp are required field");
        } else {
            JSONObject content = new JSONObject();
            for (String key : metadata.keySet()) {
                content.put(key, metadata.getString(key));
            }
            return content.toString();
        }
    }

    private static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }

    private static IllegalArgumentException illegalArgument(String message) {
        return new IllegalArgumentException(message);
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
