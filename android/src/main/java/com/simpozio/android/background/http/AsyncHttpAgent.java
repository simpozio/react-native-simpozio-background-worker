package com.simpozio.android.background.http;

import android.os.Bundle;

import java.util.concurrent.atomic.AtomicReference;

import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.event.Events;

import okhttp3.*;

public abstract class AsyncHttpAgent extends Thread implements EventPublisher {

    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");

    public final AtomicReference<Bundle> requestBody = new AtomicReference<>(null);
    public final AtomicReference<Bundle> headers = new AtomicReference<>(null);
    public final AtomicReference<String> url = new AtomicReference<>(null);

    public long next = 5000L;

    private boolean failed = false;

    private long lastFailed = 0;

    private final EventPublisher eventPublisher;

    public AsyncHttpAgent(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public abstract Request prepareRequest() throws Exception;

    @Override
    public void start() {
        try {
            super.start();
        } catch (IllegalThreadStateException ignored) {
            this.fireEvent(Events.heartbeatStartFailed(illegalState("unexpected state on start: " + getState())));
        }
    }

    @Override
    public void run() {

        long startupPoint = started();

        OkHttpClient httpClient = new OkHttpClient();

        while (!isInterrupted()) {
            try {
                Response response = httpClient.newCall(pingRequest).execute();

                long doubleRequestRoundTrip = (response.receivedResponseAtMillis() - response.sentRequestAtMillis()) * 2;

                if (response.isSuccessful()) {
                    this.onSuccess();
                } else {
                    this.onHeartbeatFailed(response.code(), response.message());
                }
                try {
                    response.close();
                } catch (Exception cause) {
                    this.onException(cause);
                }
                if (doubleRequestRoundTrip < this.next) {
                    Thread.sleep(this.next - doubleRequestRoundTrip);
                }
            } catch (InterruptedException ignored) {
                this.interrupt();
            } catch (Throwable cause) {
                this.onHeartbeatFailed(cause);
            }
        }
        this.finish(System.currentTimeMillis() - startupPoint);
    }

    @Override
    public void interrupt() {
        if (isInterrupted()) {
            this.fireEvent(Events.heartbeatStopFailed(illegalState("already interrupted")));
        } else if (!isAlive()) {
            this.fireEvent(Events.heartbeatStopFailed(illegalState("httpAgent died")));
        } else {
            super.interrupt();
        }
    }

    @Override
    public void fireEvent(Bundle event) {
        this.eventPublisher.fireEvent(event);
    }

    private long started() {
        this.fireEvent(Events.heartbeatStarted());
        return System.currentTimeMillis();
    }

    private void finish(long uptime) {
        this.fireEvent(Events.heartbeatStopped(uptime));
    }

    private void onSuccess() {
        if (this.failed) {
            this.fireEvent(Events.heartbeatResume(System.currentTimeMillis() - lastFailed));
            this.failed = false;
        }
    }

    private void onHeartbeatFailed(int code, String message) {
        if (!failed) {
            this.fireEvent(Events.heartbeatFailed(code, message));
            this.failed = true;
        }
        this.lastFailed = System.currentTimeMillis();
    }

    private void onHeartbeatFailed(Throwable cause) {
        if (!failed) {
            this.fireEvent(Events.heartbeatFailed(cause));
            this.failed = true;
        }
        this.lastFailed = System.currentTimeMillis();
    }

    private void onException(Exception cause) {
        if (!failed) {
            this.fireEvent(Events.heartbeatException(cause));
            this.failed = true;
        }
        this.lastFailed = System.currentTimeMillis();
    }

    protected static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }

    protected static IllegalArgumentException illegalArgument(String message) {
        return new IllegalArgumentException(message);
    }

}
