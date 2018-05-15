package com.simpozio.android.heartbeat;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public final class NativeHeartbeatSender extends ReactContextBaseJavaModule {

    private final HeartbeatRunner heartbeatRunner;

    public NativeHeartbeatSender(ReactApplicationContext context) {
        super(context);
        DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter = context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.heartbeatRunner = new HeartbeatRunner(eventEmitter);
    }

    @Override
    public String getName() {
        return "NativeHeartbeatSender";
    }

    @ReactMethod
    public void start(ReadableMap metadata) {
        this.updateMetadata(metadata);
        this.heartbeatRunner.start();
    }

    @ReactMethod
    public void stop() {
        this.heartbeatRunner.interrupt();
    }

    @ReactMethod
    public void set(ReadableMap metadata) {
        this.updateMetadata(metadata);
    }

    private void updateMetadata(ReadableMap metadata) {
        this.heartbeatRunner.metadata.set(metadata);
    }
}
