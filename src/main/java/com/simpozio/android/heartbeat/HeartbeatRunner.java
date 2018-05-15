package com.simpozio.android.heartbeat;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.concurrent.atomic.AtomicReference;

import com.facebook.react.bridge.*;

import org.json.*;

import okhttp3.*;

import static com.simpozio.android.heartbeat.Events.EVENT_TYPE;

public final class HeartbeatRunner extends Thread {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");

    private static final long HEARTBEAT_PERIOD_MS = 3000;

    public static final String[] REQUEST_BODY_FIELDS = {
            "touchpoint",
            "state",
            "screen",
            "connection",
            "bandwidth",
            "payload",
            "next"
    };

    public static final String[] HEADER_FIELDS = {
            "Authorization",
            "User-Agent",
            "Accept-Language",
            "X-HTTP-Method-Override"
    };

    public final AtomicReference<ReadableMap> metadata = new AtomicReference<>(null);

    private final DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;

    private boolean failed = false;

    public HeartbeatRunner(DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter) {
        this.eventEmitter = eventEmitter;
    }

    @Override
    public void start() {
        try {
            super.start();
        } catch (IllegalThreadStateException ignored) {
            IllegalStateException cause = new IllegalStateException("unexpected state on start: " + this.getState());
            this.fireEvent(Events.startFailed(cause));
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
                    Response response = httpClient.newCall(prepareRequest(this.metadata.get())).execute();
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
                Thread.sleep(HEARTBEAT_PERIOD_MS);
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

    private void fireEvent(WritableMap event) {
        this.eventEmitter.emit(event.getString(EVENT_TYPE), event);
    }


    private static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }

    private static Request prepareRequest(ReadableMap metadata) throws JSONException {

        String now = Date.now();

        if (metadata == null) {
            throw new IllegalStateException("metadata is null");
        }

        Request.Builder requestBuilder = new Request.Builder();

        for (String headerField : HEADER_FIELDS) {
            requestBuilder.header(headerField, metadata.getString(headerField));
        }

        requestBuilder.header("Date", now);

        return requestBuilder
                .url(metadata.getString("url"))
                .post(RequestBody.create(MEDIA_TYPE, prepareRequestBodyContent(metadata, now))) // pass the same atomic
                .build();
    }

    private static String prepareRequestBodyContent(ReadableMap metadata, String timestamp) throws JSONException {

        JSONObject content = new JSONObject();

        for (String requestBodyField : REQUEST_BODY_FIELDS) {
            content.put(requestBodyField, metadata.getString(requestBodyField));
        }

        return content
                .put("timestamp", timestamp)
                .toString();
    }
}
