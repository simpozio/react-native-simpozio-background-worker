package com.simpozio.android.background.heartbeat;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.simpozio.android.background.ServiceURL;
import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.http.AsyncHttpAgent;

import static com.simpozio.android.background.SimpozioJavaService.FEEDBACK_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioJavaService.FEEDBACK_INTENT_ACTION;
import static com.simpozio.android.background.SimpozioJavaService.HEADERS_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioJavaService.REQ_BODY_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioJavaService.SIMPOZIO_ADDRESS_EXTRA;

public final class HeartbeatService extends Service implements EventPublisher {

    private final AsyncHttpAgent httpAgent = new HeartbeatHttpAgent(this);

    private final BroadcastReceiver receiver = this.createReceiver();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.updateAgent(intent);
        this.httpAgent.simpozioAddress.set(intent.getStringExtra(SIMPOZIO_ADDRESS_EXTRA));
        this.httpAgent.url.set(ServiceURL.HEARTBEAT_URL); // hardcode ?
        this.httpAgent.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        this.httpAgent.interrupt();
    }

    @Override
    public void fireEvent(Bundle event) {
        this.sendBroadcast(asFeedback(event));
    }

    private void updateAgent(Intent intent) {
        this.httpAgent.headers.set(intent.getBundleExtra(HEADERS_EVENT_BUNDLE));
        this.httpAgent.requestBody.set(intent.getBundleExtra(REQ_BODY_EVENT_BUNDLE));
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                HeartbeatService.this.updateAgent(intent);
            }
        };
    }

    private static Intent asFeedback(Bundle eventBundle) {
        Intent intent = new Intent(FEEDBACK_INTENT_ACTION);
        intent.putExtra(FEEDBACK_EVENT_BUNDLE, eventBundle);
        return intent;
    }
}
