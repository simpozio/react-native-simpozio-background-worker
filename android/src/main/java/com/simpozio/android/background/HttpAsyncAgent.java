package com.simpozio.android.background;

import com.facebook.react.bridge.ReadableMap;

public interface HttpAsyncAgent {
    void start(String url, ReadableMap headers, ReadableMap requestBody);
    void update(String url, ReadableMap headers, ReadableMap requestBody);
    void stop();
}
