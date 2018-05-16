package com.simpozio.android.background.trace;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TraceService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
