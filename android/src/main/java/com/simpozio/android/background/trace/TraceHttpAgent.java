package com.simpozio.android.background.trace;

import com.simpozio.android.background.event.EventPublisher;
import com.simpozio.android.background.http.AsyncHttpAgent;

import org.json.JSONException;

import okhttp3.Request;

public final class TraceHttpAgent extends AsyncHttpAgent {

    public TraceHttpAgent(EventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public Request prepareRequest() throws JSONException {
        throw new UnsupportedOperationException();
    }
}
