package com.simpozio.android.heartbeat;

import android.annotation.SuppressLint;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class HeartbeatRunner extends Thread {

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
            "Date",
            "User-Agent",
            "Accept-Language",
            "X-HTTP-Method-Override"
    };

    public final AtomicReference<Callback> onResumeCallback = new AtomicReference<>(null);
    public final AtomicReference<Callback> onFailCallback = new AtomicReference<>(null);
    public final AtomicReference<ReadableMap> metadata = new AtomicReference<>(null);
    public final AtomicReference<Promise> startPromise = new AtomicReference<>(null);
    public final AtomicReference<Promise> stopPromise = new AtomicReference<>(null);

    private boolean failed = false;

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
                        this.onFail(unsuccessfulResponse(response.code(), response.message()));
                    } else {
                        response.close();
                        this.onSuccess(lastFailed);
                    }
                } catch (Exception e) {
                    this.onFail(e.getMessage());
                    lastFailed = System.currentTimeMillis();
                }
                Thread.sleep(HEARTBEAT_PERIOD_MS);
            } catch (InterruptedException ignored) {
                this.interrupt(); // set flag
            }
        }
        this.finished(System.currentTimeMillis() - startupPoint);
    }


    public void start(Promise promise) {
        if (this.onFailCallback.get() != null) {
            if (this.onResumeCallback.get() != null) {
                State preStartPointState = this.getState();
                try {
                    this.startPromise.set(promise);
                    this.start();
                } catch (IllegalThreadStateException ignored) {
                    promise.reject(illegalState("Unexpected state on start: " + preStartPointState));
                }
            } else {
                promise.reject(illegalState("onResume callback is not initialized"));
            }
        } else {
            promise.reject(illegalState("onFail callback is not initialized"));
        }
    }

    public void stop(Promise promise) {
        if (isInterrupted()) {
            promise.reject(illegalState("Already interrupted"));
        } else if (!isAlive()) {
            promise.reject(illegalState("HeartbeatRunner died"));
        } else {
            this.stopPromise.set(promise);
            this.interrupt();
        }
    }


    private long started() {
        this.startPromise.get().resolve("HeartbeatRunner started");
        return System.currentTimeMillis();
    }

    private void finished(long uptimeDuration) {
        this.stopPromise.get().resolve(uptimeDuration);
    }


    private void onSuccess(long lastFailed) {
        if (this.failed) {
            this.onResumeCallback
                    .get()
                    .invoke(System.currentTimeMillis() - lastFailed); // duration after last failed
            this.failed = false;
        }
    }

    private void onFail(String message) {
        if (!this.failed) {
            this.onFailCallback
                    .get()
                    .invoke(message);
            this.failed = true;
        }
    }


    private static Request prepareRequest(ReadableMap metadata) throws JSONException {

        Request.Builder requestBuilder = new Request.Builder();

        for (String headerField : HEADER_FIELDS) {
            requestBuilder.header(headerField, metadata.getString(headerField));
        }

        return requestBuilder
                .url(metadata.getString("url"))
                .post(RequestBody.create(MEDIA_TYPE, prepareRequestBodyContent(metadata))) // pass the same atomic
                .build();
    }

    private static String prepareRequestBodyContent(ReadableMap metadata) throws JSONException {

        JSONObject content = new JSONObject();

        for (String requestBodyField : REQUEST_BODY_FIELDS) {
            content.put(requestBodyField, metadata.getString(requestBodyField));
        }
        return content
                .put("timestamp", DATE_FORMAT.format(Calendar.getInstance().getTime()))
                .toString();
    }

    private static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }

    private static String unsuccessfulResponse(int code, String message) {
        return code + ", " + message;
    }

}
