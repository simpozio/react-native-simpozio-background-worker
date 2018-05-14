package com.simpozio.android.heartbeat;

import com.facebook.react.bridge.*;

public final class NativeHeartbeatSender extends ReactContextBaseJavaModule {

    private final HeartbeatRunner heartbeatRunner = new HeartbeatRunner();

    public NativeHeartbeatSender(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "NativeHeartbeatSender";
    }

    @ReactMethod
    public void onResume(Callback callback) {
        if (callback != null) {
            this.heartbeatRunner.onResumeCallback.set(callback);
        } else {
            throw new IllegalArgumentException("callback is null");
        }
    }

    @ReactMethod
    public void onFail(Callback callback) {
        if (callback != null) {
            this.heartbeatRunner.onFailCallback.set(callback);
        } else {
            throw new IllegalArgumentException("callback is null");
        }
    }

    @ReactMethod
    public void start(ReadableMap metadata, Promise promise) {
        this.updateMetadata(metadata);
        if (promise != null) {
            this.heartbeatRunner.start(promise);
        } else {
            throw new IllegalArgumentException("promise is null");
        }
    }

    @ReactMethod
    public void stop(Promise promise) {
        if (promise != null) {
            this.heartbeatRunner.stop(promise);
        } else {
            throw new IllegalArgumentException("promise is null");
        }
    }

    @ReactMethod
    public void set(ReadableMap metadata) {
        this.updateMetadata(metadata);
    }

    private void updateMetadata(ReadableMap metadata) {
        if (metadata != null) {
            this.heartbeatRunner.metadata.set(metadata);
        } else {
            throw new IllegalArgumentException("metadata is null");
        }
    }
}
