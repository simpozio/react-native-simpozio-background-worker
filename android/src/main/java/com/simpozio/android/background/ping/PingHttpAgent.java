package com.simpozio.android.background.ping;

import android.os.Bundle;

import com.simpozio.android.background.ServiceURL;
import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.event.Events;

import org.joda.time.DateTime;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PingHttpAgent extends Thread implements EventPublisher, ServiceURL {

    public final AtomicLong pingDelay = new AtomicLong();
    public final AtomicInteger pingCount = new AtomicInteger();
    public final AtomicLong pingSeriesDelay = new AtomicLong();
    public final AtomicReference<String> pingUrl = new AtomicReference<>();

    private final EventPublisher eventPublisher;

    private boolean failed = false;

    private long lastFailed = 0;

    public PingHttpAgent(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void start() {
        try {
            super.start();
        } catch (IllegalThreadStateException ignored) {
            this.fireEvent(Events.pingStartFailed(illegalState("unexpected state on start: " + getState())));
        }
    }

    @Override
    public void interrupt() {
        if (isInterrupted()) {
            this.fireEvent(Events.pingStopFailed(illegalState("already interrupted")));
        } else if (!isAlive()) {
            this.fireEvent(Events.pingStopFailed(illegalState("httpAgent died")));
        } else {
            super.interrupt();
        }
    }

    @Override
    public void run() {

        long startupPoint = started();

        Request pingRequest = new Request.Builder().get().url(pingUrl.get() + PING_URL).build();

        OkHttpClient httpClient = new OkHttpClient();

        while (!interrupted()) {
            try {

                int pingCount = this.pingCount.get();
                long pingDelay = this.pingDelay.get();
                long pingSeriesDelay = this.pingSeriesDelay.get();

                retry : {

                    Average average = new Average(pingCount);

                    for (int i = 0; i < pingCount; i += 1) {
                        Response response = httpClient.newCall(pingRequest).execute();
                        if (response.isSuccessful()) {
                            average.add(response.receivedResponseAtMillis() - response.sentRequestAtMillis());
                            this.onSuccess();
                        } else {
                            i -= 1;
                            this.onPingFailed(response.code(), response.message());
                        }
                        try {
                            response.close();
                        } catch (Exception cause) {
                            this.onException(cause);
                        }
                        Thread.sleep(pingDelay);
                    }

                    int avg = average.value();

                    int retryCount = 5;

                    repeatRequest : {

                        Response response = httpClient.newCall(pingRequest).execute();

                        long roundTrip = response.receivedResponseAtMillis() - response.sentRequestAtMillis();

                        if ((roundTrip / avg) > 1.3) {
                            try {
                                response.close();
                            } catch (Exception e) {
                                this.onException(e);
                            }
                            if ((retryCount -= 1) <= 0) {
                                break retry;
                            } else {
                                break repeatRequest;
                            }
                        }
                        try {
                            JSONObject body = new JSONObject(response.body().string());
                            fireEvent(Events.serverTimestamp(DateTime.parse(body.getString("timestamp")).plusMillis(avg / 2), avg));
                            response.close();
                        } catch (Exception e) {
                            this.onException(e);
                        }
                    }
                }
                Thread.sleep(pingSeriesDelay);
            } catch (InterruptedException ignored) {
                this.interrupt();
            } catch (Throwable cause) {
                this.onPingFailed(cause);
            }
        }
        this.finish(System.currentTimeMillis() - startupPoint);
    }

    @Override
    public void fireEvent(Bundle event) {
        this.eventPublisher.fireEvent(event);
    }

    private void onSuccess() {
        if (this.failed) {
            this.fireEvent(Events.pingResume(System.currentTimeMillis() - lastFailed));
            this.failed = false;
        }
    }

    private void onPingFailed(Throwable cause) {
        if (!failed) {
            this.fireEvent(Events.pingFailed(cause));
            this.failed = true;
        }
        this.lastFailed = System.currentTimeMillis();
    }

    private void onPingFailed(int code, String message) {
        if (!failed) {
            this.fireEvent(Events.pingFailed(code, message));
            this.failed = true;
        }
        this.lastFailed = System.currentTimeMillis();
    }

    private void onException(Exception cause) {
        if (!failed) {
            this.fireEvent(Events.pingException(cause));
            this.failed = true;
        }
        this.lastFailed = System.currentTimeMillis();
    }

    private long started() {
        this.fireEvent(Events.pingStarted());
        return System.currentTimeMillis();
    }

    private void finish(long uptime) {
        this.fireEvent(Events.pingStopped(uptime));
    }

    public static class Average {

        private final List<Long> series;

        public Average(int n) {
            this.series = new ArrayList<>(n);
        }

        public void add(long sample) {
            this.series.add(sample);
        }

        private double dev(double avg) {

            double dev = 0.0D;

            for (long sample : series) {
                dev += Math.pow(sample - avg, 2);
            }

            return Math.sqrt(dev / series.size());
        }

        private double avg() {
            long total = 0L;
            for (Long sample : series) {
                total += sample;
            }
            return (total / series.size());
        }

        public int value() {

            double avg = avg();

            if ((dev(avg) / avg) > 0.3) {
                long total = 0L;
                int count = 0;
                for (long sample : series) {
                    if ((sample / avg) <= 1.3) {
                        total += sample;
                        count += 1;
                    }
                }
                avg = (total / count);
            }

            return (int) avg;
        }
    }

    protected static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }
}
