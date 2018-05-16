package com.simpozio.android.background;

import android.annotation.SuppressLint;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.simpozio.android.background.heartbeat.*;
import com.facebook.react.bridge.*;
import android.content.*;
import android.os.*;

import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

import static com.simpozio.android.background.heartbeat.Events.*;
import static com.simpozio.android.background.ServiceURL.*;

public class SimpozioNativeBackgroundService extends ReactContextBaseJavaModule {

    public static final String FEEDBACK_EVENT_BUNDLE = "feedback.event.bundle";
    public static final String HEADERS_EVENT_BUNDLE = "headers.event.bundle";
    public static final String REQ_BODY_EVENT_BUNDLE = "request.body.event.bundle";
    public static final String FEEDBACK_INTENT_ACTION = "background.service.feedback";
    public static final String HEARTBEAT_INTENT_ACTION = "background.service.heartbeat";
    public static final String TRACE_INTENT_ACTION = "background.service.trace";

    private final DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private final PowerManager.WakeLock wakeLock;

    @SuppressLint("WakelockTimeout")
    public SimpozioNativeBackgroundService(ReactApplicationContext context) {
        super(context);
        this.wakeLock = getWakeLock();
        this.eventEmitter = getEventEmitter();
        this.wakeLock.acquire();
        //
        context.registerReceiver(createReceiver(), getIntentFilter());
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WritableMap event = toWritableMap(intent.getBundleExtra(FEEDBACK_EVENT_BUNDLE));
                eventEmitter.emit(event.getString(EVENT_TYPE), event);
            }
        };
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @ReactMethod
    public void start(String url) {
        switch (url) {
            case TRACE_URL:
                this.startTraceService();
                break;
            case HEARTBEAT_URL:
                this.startHeartbeatService();
                break;
            default: {
                this.fireEvent(Events.unknownUrl(url));
            }
        }
    }

    @ReactMethod
    public void stop(String url) {
        switch (url) {
            case TRACE_URL:
                this.stopTraceService();
                break;
            case HEARTBEAT_URL:
                this.stopHeartbeatService();
                break;
            default: {
                this.fireEvent(Events.unknownUrl(url));
            }
        }
    }

    @ReactMethod
    public void setData(String url, ReadableMap headers, ReadableMap requestBody) {
        switch (url) {
            case TRACE_URL:
                this.sendBroadcast(toTraceActionIntent(null, null));
                break;
            case HEARTBEAT_URL:
                this.sendBroadcast(toHeartbeatActionIntent(headers, requestBody));
                break;
            default: {
                this.fireEvent(Events.unknownUrl(url));
            }
        }
    }

    @ReactMethod
    public void releaseWakeLock() {
        this.wakeLock.release();
    }

    // TODO: implement trace service

    private Intent toTraceActionIntent(ReadableMap headers, ReadableMap requestBody) {
        throw new UnsupportedOperationException();
    }

    private Intent toHeartbeatActionIntent(ReadableMap headers, ReadableMap requestBody) {
        Intent metadataIntent = new Intent(HEARTBEAT_INTENT_ACTION);
        acceptExtra(headers, requestBody, metadataIntent);
        return metadataIntent;
    }

    private void sendBroadcast(Intent intent) {
        this.getReactApplicationContext().sendBroadcast(intent);
    }

    private void startTraceService() {
        this.getReactApplicationContext().startService(getTraceServiceIntent());
    }

    private void startHeartbeatService() {
        this.getReactApplicationContext().startService(getHeartbeatServiceIntent());
    }

    private void stopTraceService() {
        this.getReactApplicationContext().stopService(getTraceServiceIntent());
    }

    private void stopHeartbeatService() {
        this.getReactApplicationContext().stopService(getHeartbeatServiceIntent());
    }

    private void fireEvent(Bundle bundle) {
        WritableMap event = Events.toWritableMap(bundle);
        this.eventEmitter.emit(event.getString(EVENT_TYPE), event);
    }

    private DeviceEventManagerModule.RCTDeviceEventEmitter getEventEmitter() {
        return this.getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    }

    private PowerManager.WakeLock getWakeLock() {
        return ((PowerManager) this.getReactApplicationContext()
                .getSystemService(POWER_SERVICE))
                .newWakeLock(PARTIAL_WAKE_LOCK, "wl");
    }

    private Intent getHeartbeatServiceIntent() {
        return new Intent(this.getReactApplicationContext(), HeartbeatService.class);
    }

    private Intent getTraceServiceIntent() {
        return new Intent(this.getReactApplicationContext(), HeartbeatService.class);
    }

    private static void acceptExtra(ReadableMap headers, ReadableMap requestBody, Intent metadataIntent) {
        // headers
        ReadableMapKeySetIterator headerKeys = headers.keySetIterator();
        Bundle headersEventBundle = new Bundle();
        while (headerKeys.hasNextKey()) {
            String key = headerKeys.nextKey();
            headersEventBundle.putString(key, headers.getString(key));
        }
        metadataIntent.putExtra(HEADERS_EVENT_BUNDLE, headersEventBundle);
        // req body
        ReadableMapKeySetIterator reqBodyKeys = headers.keySetIterator();
        Bundle reqBodyEventBundle = new Bundle();
        while (reqBodyKeys.hasNextKey()) {
            String key = reqBodyKeys.nextKey();
            reqBodyEventBundle.putString(key, headers.getString(key));
        }
        metadataIntent.putExtra(REQ_BODY_EVENT_BUNDLE, headersEventBundle);
    }

    private static IntentFilter getIntentFilter() {
        return new IntentFilter(FEEDBACK_INTENT_ACTION);
    }
}
