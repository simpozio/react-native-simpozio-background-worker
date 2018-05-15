package com.simpozio.android.background;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.PowerManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.simpozio.android.background.heartbeat.HeartbeatRunner;
import com.simpozio.android.background.heartbeat.HeartbeatService;

import java.util.concurrent.atomic.AtomicReference;

import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public final class SimpozioNativeBackgroundService extends ReactContextBaseJavaModule {

    private final static AtomicReference<HeartbeatRunner> HEARTBEAT_RUNNER = new AtomicReference<>(null);

    private final PowerManager.WakeLock heartbeatWakeLock;

    public SimpozioNativeBackgroundService(ReactApplicationContext context, Intent heartbeatServiceIntent) {
        super(context);
        this.heartbeatWakeLock = getHeartbeatWakeLock();
        this.initializeHeartbeatRunner();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @ReactMethod
    @SuppressLint("WakelockTimeout")
    public void startHeartbeat(ReadableMap metadata) {
        this.heartbeatWakeLock.acquire();
        getHeartbeatRunner().metadata.set(metadata);
        this.getReactApplicationContext().startService(this.getHeartbeatServiceIntent());
    }

    @ReactMethod
    public void stopHeartbeat() {
        this.getReactApplicationContext().stopService(this.getHeartbeatServiceIntent());
        this.heartbeatWakeLock.release();
    }

    @ReactMethod
    public void setHeartbeatData(ReadableMap metadata) {
        getHeartbeatRunner().metadata.set(metadata);
    }

    private PowerManager.WakeLock getHeartbeatWakeLock() {
        return ((PowerManager) this.getReactApplicationContext()
                .getSystemService(POWER_SERVICE))
                .newWakeLock(PARTIAL_WAKE_LOCK, "hbwl");
    }

    private void initializeHeartbeatRunner() {
        SimpozioNativeBackgroundService.HEARTBEAT_RUNNER.set(createHeartbeatRunner());
    }

    private HeartbeatRunner createHeartbeatRunner() {
        ReactApplicationContext context = this.getReactApplicationContext();
        DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter = context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        return new HeartbeatRunner(eventEmitter);
    }

    private Intent getHeartbeatServiceIntent() {
        return new Intent(this.getReactApplicationContext(), HeartbeatService.class);
    }

    public static HeartbeatRunner getHeartbeatRunner() {
        HeartbeatRunner heartbeatRunner = HEARTBEAT_RUNNER.get();
        if (heartbeatRunner != null) {
            return heartbeatRunner;
        } else {
            throw new IllegalStateException("HeartbeatRunner is not initialized");
        }
    }
}
