package com.simpozio.android.background.ping;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;

import com.simpozio.android.background.event.EventPublisher;

import static com.simpozio.android.background.SimpozioBackgroundWorker.FEEDBACK_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioBackgroundWorker.FEEDBACK_INTENT_ACTION;
import static com.simpozio.android.background.SimpozioBackgroundWorker.PING_INTENT_ACTION;

public class PingService extends Service implements EventPublisher {

    private final PingHttpAgent pingAgent = new PingHttpAgent(this);

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerReceiver(createReceiver(), getPingIntentFilter());
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.updateAgent(intent);
        this.pingAgent.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        this.pingAgent.interrupt();
    }

    @Override
    public void fireEvent(Bundle event) {
        this.sendBroadcast(asFeedback(event));
    }

   private void updateAgent(Intent intent) {
       this.pingAgent.pingCount.set(intent.getIntExtra("count", 10));
       this.pingAgent.pingDelay.set(intent.getLongExtra("delay", 5000)); // 5 sec
       this.pingAgent.pingSeriesDelay.set(intent.getLongExtra("seriesDelay", 300000)); // 10 min
       this.pingAgent.pingUrl.set(intent.getStringExtra("baseUrl"));
   }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PingService.this.updateAgent(intent);
            }
        };
    }

    private static Intent asFeedback(Bundle eventBundle) {
        Intent intent = new Intent(FEEDBACK_INTENT_ACTION);
        intent.putExtra(FEEDBACK_EVENT_BUNDLE, eventBundle);
        return intent;
    }

    private static IntentFilter getPingIntentFilter() {
        return new IntentFilter(PING_INTENT_ACTION);
    }
}
