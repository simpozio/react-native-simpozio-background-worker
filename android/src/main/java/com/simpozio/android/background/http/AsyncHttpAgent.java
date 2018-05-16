package com.simpozio.android.background.http;

import android.os.Bundle;

import org.json.JSONException;

import java.util.concurrent.atomic.AtomicReference;

import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.event.Events;

import okhttp3.*;

public abstract class AsyncHttpAgent extends Thread implements EventPublisher {

    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");

    public final AtomicReference<String> simpozioAddress = new AtomicReference<>(null);
    public final AtomicReference<Bundle> requestBody = new AtomicReference<>(null);
    public final AtomicReference<Bundle> headers = new AtomicReference<>(null);
    public final AtomicReference<String> url = new AtomicReference<>(null);

    public volatile long eventLoopPeriodMs = 5000L;

    private boolean failed = false;

    private final EventPublisher eventPublisher;

    public AsyncHttpAgent(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public abstract Request prepareRequest() throws JSONException;

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
                Thread.sleep(this.eventLoopPeriodMs);
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

    protected static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }

    protected static IllegalArgumentException illegalArgument(String message) {
        return new IllegalArgumentException(message);
    }

}
