package com.simpozio.android.background.event;

import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.simpozio.android.background.heartbeat.DateFormatted;

public final class Events {

    public static final String EVENT_TYPE = "type";

    private static final String HEARTBEAT_FAILED = "heartbeatFailed";
    private static final String START_FAILED = "startFailed";
    private static final String STOP_FAILED = "stopFailed";
    private static final String UNKNOWN_URL = "unknownUrl";
    private static final String EXCEPTION = "exception";
    private static final String STARTED = "started";
    private static final String STOPPED = "stopped";
    private static final String RESUME = "resume";

    private static final String TIMESTAMP_FIELD = "timestamp";

    private Events() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "exception", // discriminator
     *     "timestamp" : "string",    // event timestamp
     *     "cause"     : "string",    // Exception type canonical name
     *     "message"   : "string"     // Exception comment message
     * }
     */

    public static Bundle exception(Exception cause) {
        Bundle event = createEvent(EXCEPTION);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return event;
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "resume",  // discriminator
     *     "timestamp" : "string",  // event timestamp
     *     "duration"  : "string"   // duration from last fail
     * }
     */

    public static Bundle resume(long durationFromLastFail) {
        Bundle event = createEvent(RESUME);
        event.putString("duration", String.valueOf(durationFromLastFail));
        return acceptTimestamp(event);
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "started",   // discriminator
     *     "timestamp" : "string"    // event timestamp
     * }
     */

    public static Bundle started() {
        return createEvent(STARTED);
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopped", // discriminator
     *     "timestamp" : "string",  // event timestamp
     *     "uptime"    : "string"   // uptime duration of the HeartbeatRunner
     * }
     */

    public static Bundle stopped(long uptime) {
        Bundle event = createEvent(STOPPED);
        event.putString("uptime", String.valueOf(uptime));
        return event;
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "startFailed", // discriminator
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle startFailed(Exception cause) {
        Bundle event = createEvent(START_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return event;
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopFailed",  // discriminator
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle stopFailed(Exception cause) {
        Bundle event = createEvent(STOP_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return event;
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "unknownUrl",  // discriminator
     *     "timestamp" : "string",      // event timestamp
     *     "url"       : "string",
     * }
     */

    public static Bundle unknownUrl(String url) {
        Bundle event = createEvent(UNKNOWN_URL);
        event.putString("url", url);
        return acceptTimestamp(event);
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "unsuccessfullyResponse", // discriminator
     *     "timestamp" : "string",                 // event timestamp
     *     "message"   : "string",                 // response message or exception comment message
     *     "code"      : "string",                 // response code or -1
     *     "cause"     : "string"                  // Exception type canonical name or empty string
     * }
     */

    public static Bundle heartbeatFailed(int code, String message) {
        Bundle event = createEvent(HEARTBEAT_FAILED);
        event.putString("code", String.valueOf(code));
        event.putString("message", message);
        event.putString("cause", "");
        return event;
    }

    public static Bundle heartbeatFailed(Exception cause) {
        Bundle event = createEvent(HEARTBEAT_FAILED);
        event.putString("code", "-1");
        event.putString("message", cause.getMessage());
        event.putString("cause", cause.getClass().getCanonicalName());
        return event;
    }

    public static WritableMap toWritableMap(Bundle eventBundle) {
        WritableMap event = Arguments.createMap();
        for (String key : eventBundle.keySet()) {
            event.putString(key, eventBundle.getString(key));
        }
        return event;
    }

    private static Bundle createEvent(String type) {
        Bundle event = new Bundle();
        event.putString(EVENT_TYPE, type);
        return acceptTimestamp(event);
    }

    private static Bundle acceptTimestamp(Bundle event) {
        event.putString(TIMESTAMP_FIELD, DateFormatted.now().timestamp());
        return event;
    }
}
