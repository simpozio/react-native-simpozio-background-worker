package com.simpozio.android.background.heartbeat;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.IBinder;

import com.simpozio.android.background.ServiceURL;

import java.io.InputStream;
import java.util.Properties;

import static com.simpozio.android.background.SimpozioNativeBackgroundService.FEEDBACK_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioNativeBackgroundService.FEEDBACK_INTENT_ACTION;
import static com.simpozio.android.background.SimpozioNativeBackgroundService.HEADERS_EVENT_BUNDLE;
import static com.simpozio.android.background.SimpozioNativeBackgroundService.REQ_BODY_EVENT_BUNDLE;

public class HeartbeatService extends Service implements EventPublisher {

    private final HeartbeatRunner heartbeatRunner = new HeartbeatRunner(this);

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateRunner(intent);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.updateRunner(intent);
        this.heartbeatRunner.url.set(ServiceURL.HEARTBEAT_URL); // hardcode ?
        this.heartbeatRunner.simpozioAddress.set(getSimpozioAddress());
        this.heartbeatRunner.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        this.heartbeatRunner.interrupt();
    }

    @Override
    public void fireEvent(Bundle event) {
        this.sendBroadcast(asFeedback(event));
    }

    private void updateRunner(Intent intent) {
        this.heartbeatRunner.headers.set(intent.getBundleExtra(HEADERS_EVENT_BUNDLE));
        this.heartbeatRunner.requestBody.set(intent.getBundleExtra(REQ_BODY_EVENT_BUNDLE));
    }

    private String getSimpozioAddress() {
        try {
            Properties properties = new Properties();
            AssetManager assetManager = this.getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            try {
                properties.load(inputStream);
            } finally {
                inputStream.close();
            }
            return properties.getProperty("simpozio.address");
        } catch (Exception e) {
            throw new RuntimeException("cannot read properties: " + e.getMessage());
        }
    }

    private static Intent asFeedback(Bundle eventBundle) {
        Intent intent = new Intent(FEEDBACK_INTENT_ACTION);
        intent.putExtra(FEEDBACK_EVENT_BUNDLE, eventBundle);
        return intent;
    }
}
