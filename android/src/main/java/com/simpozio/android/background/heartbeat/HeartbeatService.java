package com.simpozio.android.background.heartbeat;

import android.app.Service;
import android.content.*;

import android.os.*;
import android.util.Log;

import com.simpozio.android.background.event.BridgeBroadcastReceiver;
import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.http.AsyncHttpAgent;

import static com.simpozio.android.background.SimpozioJavaService.*;

public final class HeartbeatService extends Service implements EventPublisher {

    private static final String LOG_TAG = "HeartbeatService";

    private final AsyncHttpAgent httpAgent = new HeartbeatHttpAgent(this);
    private final BroadcastReceiver receiver = new BridgeBroadcastReceiver(httpAgent);

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate started");
        super.onCreate();
        this.registerReceiver(receiver, getHeartbeatIntentFilter());
        Log.d(LOG_TAG, "onCreate finished");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    } // there is no binders

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand started");
        this.httpAgent.notify(intent);
        this.httpAgent.start();
        Log.d(LOG_TAG, "onStartCommand finished");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy started");
        this.httpAgent.interrupt();
        Log.d(LOG_TAG, "onDestroy finished");
    }

    @Override
    public void fireEvent(Bundle event) {
        Log.d(LOG_TAG, "fireEvent started");
        this.sendBroadcast(asFeedback(event));
        Log.d(LOG_TAG, "fireEvent finished");
    }
    
    private static Intent asFeedback(Bundle eventBundle) {
        Intent intent = new Intent(FEEDBACK_INTENT_ACTION);
        intent.putExtra(FEEDBACK_EVENT_BUNDLE, eventBundle);
        return intent;
    }

    private static IntentFilter getHeartbeatIntentFilter() {
        return new IntentFilter(HEARTBEAT_INTENT_ACTION);
    }
}
