package com.simpozio.android.background.http;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;

import java.util.concurrent.atomic.AtomicReference;

import com.simpozio.android.background.event.EventListener;
import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.event.Events;

import okhttp3.*;

public abstract class AsyncHttpAgent extends Thread implements EventPublisher, EventListener {

    private static final String LOG_TAG = "AsyncHttpAgent";

    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");

    public final AtomicReference<Bundle> requestBody = new AtomicReference<>(null);
    public final AtomicReference<Bundle> headers = new AtomicReference<>(null);
    public final AtomicReference<String> url = new AtomicReference<>(null);

    public volatile long eventLoopPeriodMs = 5000L;

    private boolean failed = false;

    private final EventPublisher feedback;

    public AsyncHttpAgent(EventPublisher eventPublisher) {
        this.feedback = eventPublisher;
    }

    public abstract Request prepareRequest() throws JSONException;

    @Override
    public void start() {
        try {
            Log.d(LOG_TAG, "start started");
            super.start();
        } catch (IllegalThreadStateException ignored) {
            this.fireEvent(Events.startFailed(illegalState("unexpected state on start: " + this.getState())));
        } finally {
            Log.d(LOG_TAG, "start finished");
        }
    }

    @Override
    public void run() {

        Log.d(LOG_TAG, "run started");

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
                Thread.sleep(this.eventLoopPeriodMs);
            } catch (InterruptedException ignored) {
                this.interrupt(); // set flag
            }
        }
        this.finish(System.currentTimeMillis() - startupPoint);

        Log.d(LOG_TAG, "run finished");
    }

    @Override
    public void interrupt() {

        Log.d(LOG_TAG, "run started");

        if (isInterrupted()) {
            this.fireEvent(Events.stopFailed(illegalState("already interrupted")));
        } else if (!isAlive()) {
            this.fireEvent(Events.stopFailed(illegalState("agent died")));
        } else {
            super.interrupt();
        }

        Log.d(LOG_TAG, "run finished");
    }

    @Override
    public void fireEvent(Bundle event) {
        Log.d(LOG_TAG, "fireEvent started");
        this.feedback.fireEvent(event);
        Log.d(LOG_TAG, "fireEvent finished");
    }

    private long started() {
        Log.d(LOG_TAG, "started started");
        this.fireEvent(Events.started());
        try {
            return System.currentTimeMillis();
        } finally {
            Log.d(LOG_TAG, "started finished");
        }
    }

    private void finish(long uptime) {
        Log.d(LOG_TAG, "finish started");
        this.fireEvent(Events.stopped(uptime));
        Log.d(LOG_TAG, "finish finished");
    }

    private void onSuccess(long lastFailed) {
        Log.d(LOG_TAG, "onSuccess started");
        if (this.failed) {
            this.fireEvent(Events.resume(System.currentTimeMillis() - lastFailed));
            this.failed = false;
        }
        Log.d(LOG_TAG, "onSuccess finished");
    }

    private void onException(Exception cause) {
        Log.d(LOG_TAG, "onException started");
        this.fireEvent(Events.exception(cause));
        Log.d(LOG_TAG, "onException finished");
    }

    private void onUnsuccessfullyResponse(int code, String message) {
        Log.d(LOG_TAG, "onUnsuccessfullyResponse started");
        if (!this.failed) {
            this.fireEvent(Events.unsuccessfullyResponse(code, message));
            this.failed = true;
        }
        Log.d(LOG_TAG, "onUnsuccessfullyResponse finished");
    }

    protected static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }

    protected static IllegalArgumentException illegalArgument(String message) {
        return new IllegalArgumentException(message);
    }

}
