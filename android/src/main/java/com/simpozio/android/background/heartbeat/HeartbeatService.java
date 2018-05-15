package com.simpozio.android.background.heartbeat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.simpozio.android.background.SimpozioNativeBackgroundService;

public class HeartbeatService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SimpozioNativeBackgroundService.getHeartbeatRunner().start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        SimpozioNativeBackgroundService.getHeartbeatRunner().interrupt();
    }
}
