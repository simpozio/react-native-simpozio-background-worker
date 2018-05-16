package com.simpozio.android.background.trace;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.http.AsyncHttpAgent;

public final class TraceService extends Service implements EventPublisher {

    private final AsyncHttpAgent httpAgent = new TraceHttpAgent(this);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void fireEvent(Bundle event) {

    }
}
