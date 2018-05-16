package com.simpozio.android.background;

import android.annotation.SuppressLint;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.simpozio.android.background.event.Events;
import com.simpozio.android.background.heartbeat.*;
import com.facebook.react.bridge.*;

import android.content.*;
import android.os.*;

import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

import static com.simpozio.android.background.event.Events.*;
import static com.simpozio.android.background.ServiceURL.*;

public final class SimpozioJavaService extends ReactContextBaseJavaModule {

    public static final String HEARTBEAT_INTENT_ACTION = "background.service.heartbeat";
    public static final String FEEDBACK_INTENT_ACTION = "background.service.feedback";
    public static final String TRACE_INTENT_ACTION = "background.service.trace";

    public static final String REQUEST_BODY_EVENT_BUNDLE = "request.body.event.bundle";
    public static final String FEEDBACK_EVENT_BUNDLE = "feedback.event.bundle";
    public static final String HEADERS_EVENT_BUNDLE = "headers.event.bundle";

    public static final String SIMPOZIO_URL_EXTRA = "simpozio.url";

    private final DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private final PowerManager.WakeLock wakeLock;

    @SuppressLint("WakelockTimeout")
    public SimpozioJavaService(ReactApplicationContext context) {
        super(context);
        this.eventEmitter = getEventEmitter();
        this.wakeLock = getWakeLock();
        this.wakeLock.acquire();
        //
        context.registerReceiver(createReceiver(), getIntentFilter());
    }

    // React Native API

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * @param metadata is object {"baseUrl":"string", "call":"string", "headers":{...}, "body":{...} or [...]}
     */

    @ReactMethod
    public void start(ReadableMap metadata) {
        String url = metadata.getString("call");
        switch (url) {
            case TRACE_URL:
                this.startTraceService(metadata);
                break;
            case HEARTBEAT_URL:
                this.startHeartbeatService(metadata);
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

    /**
     * @param metadata is object {"baseUrl":"string", "call":"string", "headers":{...}, "body":{...} or [...]}
     */

    @ReactMethod
    public void update(ReadableMap metadata) {
        String url = metadata.getString("call");
        switch (url) {
            case TRACE_URL:
                this.sendBroadcast(toTraceIntent(metadata));
                break;
            case HEARTBEAT_URL:
                this.sendBroadcast(toHeartbeatIntent(metadata));
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


    private void startTraceService(ReadableMap metadata) {
        throw new UnsupportedOperationException();
    }

    private void startHeartbeatService(ReadableMap metadata) {
        Intent heartbeatServiceIntent = getHeartbeatServiceIntent();
        acceptExtra(metadata, heartbeatServiceIntent);
        this.getReactApplicationContext().startService(heartbeatServiceIntent);
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SimpozioJavaService.this.fireEvent(intent.getBundleExtra(FEEDBACK_EVENT_BUNDLE));
            }
        };
    }

    private Intent toTraceIntent(ReadableMap metadata) {
        throw new UnsupportedOperationException();
//        Intent metadataIntent = new Intent(TRACE_INTENT_ACTION);
//        acceptExtra(metadata, metadataIntent);
//        return metadataIntent;
    }

    private Intent toHeartbeatIntent(ReadableMap metadata) {
        Intent metadataIntent = new Intent(HEARTBEAT_INTENT_ACTION);
        acceptExtra(metadata, metadataIntent);
        return metadataIntent;
    }

    private void stopTraceService() {
        throw new UnsupportedOperationException();
//        this.getReactApplicationContext().stopService(getTraceServiceIntent());
    }

    private void stopHeartbeatService() {
        this.getReactApplicationContext().stopService(getHeartbeatServiceIntent());
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

    private DeviceEventManagerModule.RCTDeviceEventEmitter getEventEmitter() {
        return this.getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    }

    private PowerManager.WakeLock getWakeLock() {
        return ((PowerManager) this.getReactApplicationContext()
                .getSystemService(POWER_SERVICE))
                .newWakeLock(PARTIAL_WAKE_LOCK, "wl");
    }

    private Intent getHeartbeatServiceIntent() {
        return new Intent(getReactApplicationContext(), HeartbeatService.class);
    }

    private Intent getTraceServiceIntent() {
        return new Intent(getReactApplicationContext(), HeartbeatService.class);
    }

    private void acceptExtra(ReadableMap metadata, Intent metadataIntent) {
        // simpozio address
        metadataIntent.putExtra(SIMPOZIO_URL_EXTRA, metadata.getString("baseUrl") + metadata.getString("call"));
        // headers
        ReadableMap headers = metadata.getMap("headers");
        ReadableMapKeySetIterator headerKeys = headers.keySetIterator();
        Bundle headersEventBundle = new Bundle();
        while (headerKeys.hasNextKey()) {
            String key = headerKeys.nextKey();
            headersEventBundle.putString(key, headers.getString(key));
        }
        metadataIntent.putExtra(HEADERS_EVENT_BUNDLE, headersEventBundle);
        // request body
        if (metadata.getType("requestBody").equals(ReadableType.Map)) {
            ReadableMap requestBody = metadata.getMap("requestBody");
            ReadableMapKeySetIterator requestBodyKeys = requestBody.keySetIterator();
            Bundle requestBodyEventBundle = new Bundle();
            while (requestBodyKeys.hasNextKey()) {
                String key = requestBodyKeys.nextKey();
                requestBodyEventBundle.putString(key, headers.getString(key));
            }
            metadataIntent.putExtra(REQUEST_BODY_EVENT_BUNDLE, headersEventBundle);
        } else {
            throw new UnsupportedOperationException(); // TODO: implement trace
        }
    }

//    private static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
//        JSONArray array = new JSONArray();
//        for (int i = 0; i < readableArray.size(); i++) {
//            switch (readableArray.getType(i)) {
//                case Null:
//                    break;
//                case Boolean:
//                    array.put(readableArray.getBoolean(i));
//                    break;
//                case Number:
//                    array.put(readableArray.getDouble(i));
//                    break;
//                case String:
//                    array.put(readableArray.getString(i));
//                    break;
//                case Map:
//                    array.put(convertMapToJson(readableArray.getMap(i)));
//                    break;
//                case Array:
//                    array.put(convertArrayToJson(readableArray.getArray(i)));
//                    break;
//            }
//        }
//        return array;
//    }

    private static IntentFilter getIntentFilter() {
        return new IntentFilter(FEEDBACK_INTENT_ACTION);
    }
}
