package com.simpozio.android.background;

import android.content.*;
import android.os.Bundle;
import android.os.PowerManager;
import android.annotation.SuppressLint;

import com.facebook.react.bridge.*;
import com.simpozio.android.background.event.Events;
import com.simpozio.android.background.ping.PingService;
import com.simpozio.android.background.trace.TraceService;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.simpozio.android.background.heartbeat.HeartbeatService;

import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

import static com.facebook.react.bridge.ReadableType.*;
import static com.simpozio.android.background.ServiceURL.*;
import static com.simpozio.android.background.event.Events.EVENT_TYPE;

public final class SimpozioBackgroundWorker extends ReactContextBaseJavaModule implements ServiceURL {

    public static final String HEARTBEAT_INTENT_ACTION = "background.service.heartbeat";
    public static final String PING_INTENT_ACTION = "background.service.ping";
    public static final String FEEDBACK_INTENT_ACTION = "background.service.feedback";
    public static final String TRACE_INTENT_ACTION = "background.service.trace";

    public static final String REQUEST_BODY_EVENT_BUNDLE = "request.body.event.bundle";
    public static final String FEEDBACK_EVENT_BUNDLE = "feedback.event.bundle";
    public static final String HEADERS_EVENT_BUNDLE = "headers.event.bundle";

    public static final String SIMPOZIO_URL_EXTRA = "simpozio.url";

    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private PowerManager.WakeLock wakeLock;

    public SimpozioBackgroundWorker(ReactApplicationContext context) {
        super(context);
    }

    @Override
    @SuppressLint({"WakelockTimeout", "InvalidWakeLockTag"})
    public void initialize() {
        this.eventEmitter = getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.wakeLock = ((PowerManager) getReactApplicationContext().getSystemService(POWER_SERVICE)).newWakeLock(PARTIAL_WAKE_LOCK, "wl");
        this.wakeLock.acquire();
        //
        this.getReactApplicationContext().registerReceiver(createReceiver(), getFeedbackIntentFilter());
    }

    // React Native API

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * @param metadata is object {"baseUrl":"string", "headers":{...}, "requestBody":{...} or [...]}
     */

    @ReactMethod
    public void startHeartbeat(ReadableMap metadata) {
        this.startHeartbeatService(metadata);
    }

    /**
     * @param metadata is object {"baseUrl":"string", "delay":"string", "seriesDelay":"string", "count":"string"}
     */

    @ReactMethod
    public void startPing(ReadableMap metadata) {
        this.startPingService(metadata);
    }

    @ReactMethod
    public void startTrace(ReadableMap metadata) {
        this.startPingService(metadata);
    }

    @ReactMethod
    public void stopHeartbeat() {
        this.stopHeartbeatService();
    }

    @ReactMethod
    public void stopPing() {
        this.stopPingService();
    }

    @ReactMethod
    public void stopTrace() {
        this.stopPingService();
    }

    /**
     * @param metadata is object {"debug":"boolean", "baseUrl":"string", "delay":"string", "seriesDelay":"string", "count":"string"}
     */

    @ReactMethod
    public void updatePing(ReadableMap metadata) {
        this.sendBroadcast(toPingIntent(metadata));
    }

    /**
     * @param metadata is object {"baseUrl":"string", "headers":{...}, "requestBody":{...} or [...]}
     */

    @ReactMethod
    public void updateHeartbeat(ReadableMap metadata) {
        this.sendBroadcast(toHeartbeatIntent(metadata));
    }

    @ReactMethod
    public void updateTrace(ReadableMap metadata) {
        this.sendBroadcast(toTraceIntent(metadata));
    }

    @ReactMethod
    public void releaseWakeLock() {
        this.wakeLock.release();
    }

    private void startTraceService(ReadableMap metadata) {
        throw new UnsupportedOperationException();
    }

    private void startHeartbeatService(ReadableMap metadata) {
        this.getReactApplicationContext().startService(acceptExtra(metadata, getHeartbeatServiceIntent(), HEARTBEAT_URL));
    }

    private void startPingService(ReadableMap metadata) {
        this.getReactApplicationContext().startService(acceptPingExtra(metadata, getPingServiceIntent()));
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SimpozioBackgroundWorker.this.fireEvent(intent.getBundleExtra(FEEDBACK_EVENT_BUNDLE));
            }
        };
    }

    private Intent toTraceIntent(ReadableMap metadata) {
        throw new UnsupportedOperationException();
//        Intent metadataIntent = new Intent(TRACE_INTENT_ACTION);
//        return acceptExtra(metadata, metadata, TRACE_URL);
    }

    private Intent toHeartbeatIntent(ReadableMap metadata) {
        Intent metadataIntent = new Intent(HEARTBEAT_INTENT_ACTION);
        return acceptExtra(metadata, metadataIntent, HEARTBEAT_URL);
    }

    private Intent toPingIntent(ReadableMap metadata) {
        Intent metadataIntent = new Intent(PING_INTENT_ACTION);
        return acceptPingExtra(metadata, metadataIntent);
    }

    private void stopTraceService() {
        throw new UnsupportedOperationException();
//        this.getReactApplicationContext().stopService(getTraceServiceIntent());
    }

    private void stopHeartbeatService() {
        this.getReactApplicationContext().stopService(getHeartbeatServiceIntent());
    }

    private void stopPingService() {
        this.getReactApplicationContext().stopService(getPingServiceIntent());
    }

    private void sendBroadcast(Intent intent) {
        this.getReactApplicationContext().sendBroadcast(intent);
    }

    private void fireEvent(Bundle event) {
        this.fireEvent(Events.toWritableMap(event));
    }

    private void fireEvent(WritableMap event) {
        this.eventEmitter.emit(event.getString(EVENT_TYPE), event);
    }

    private Intent getHeartbeatServiceIntent() {
        return new Intent(getReactApplicationContext(), HeartbeatService.class);
    }

    private Intent getPingServiceIntent() {
        return new Intent(getReactApplicationContext(), PingService.class);
    }

    private Intent getTraceServiceIntent() {
        return new Intent(getReactApplicationContext(), TraceService.class);
    }

    private static Intent acceptExtra(ReadableMap metadata, Intent metadataIntent, String path) {
        // simpozio address
        metadataIntent.putExtra(SIMPOZIO_URL_EXTRA, metadata.getString("baseUrl") + path);
        // headers
        acceptHeadersExtra(metadata, metadataIntent);
        // request body
        ReadableType requestBodyType = metadata.getType("requestBody");
        //
        if (requestBodyType.equals(Map)) {
            acceptMapRequestBodyExtra(metadata, metadataIntent);
        } else if (requestBodyType.equals(Array)) {
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
        return metadataIntent;
    }

    private static void acceptMapRequestBodyExtra(ReadableMap metadata, Intent metadataIntent) {
        ReadableMap requestBody = metadata.getMap("requestBody");
        ReadableMapKeySetIterator requestBodyKeys = requestBody.keySetIterator();
        Bundle requestBodyEventBundle = new Bundle();
        while (requestBodyKeys.hasNextKey()) {
            String key = requestBodyKeys.nextKey();
            if (key.equals("next")) {
                if (!requestBody.isNull(key)) {
                    switch (requestBody.getType(key)) {
                        case Number:
                            requestBodyEventBundle.putString(key, requestBody.getInt(key) + "ms");
                            break;
                        case String:
                            requestBodyEventBundle.putString(key, requestBody.getString(key));
                            break;
                    }
                }
            } else {
                requestBodyEventBundle.putString(key, requestBody.getString(key));
            }
        }
        metadataIntent.putExtra(REQUEST_BODY_EVENT_BUNDLE, requestBodyEventBundle);
    }

    private static Intent acceptPingExtra(ReadableMap metadata, Intent metadataIntent) {
        metadataIntent.putExtra("debug", metadata.getBoolean("debug"));
        metadataIntent.putExtra("delay", metadata.getInt("delay"));
        metadataIntent.putExtra("baseUrl", metadata.getString("baseUrl"));
        metadataIntent.putExtra("count", metadata.getInt("count"));
        metadataIntent.putExtra("seriesDelay", metadata.getInt("seriesDelay"));
        return metadataIntent;
    }

    private static void acceptHeadersExtra(ReadableMap metadata, Intent metadataIntent) {
        ReadableMap headers = metadata.getMap("headers");
        ReadableMapKeySetIterator headerKeys = headers.keySetIterator();
        Bundle headersEventBundle = new Bundle();
        while (headerKeys.hasNextKey()) {
            String key = headerKeys.nextKey();
            headersEventBundle.putString(key, headers.getString(key));
        }
        metadataIntent.putExtra(HEADERS_EVENT_BUNDLE, headersEventBundle);
    }

    private static IntentFilter getFeedbackIntentFilter() {
        return new IntentFilter(FEEDBACK_INTENT_ACTION);
    }
}
