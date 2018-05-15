package com.simpozio.android.heartbeat;

import com.facebook.react.bridge.*;

public final class Events {

    public static final String EVENT_TYPE = "type";

    private static final String UNSUCCESSFULLY_RESPONSE = "UnsuccessfullyResponse";
    private static final String START_FAILED = "StartFailed";
    private static final String STOP_FAILED = "StopFailed";
    private static final String EXCEPTION = "Exception";
    private static final String STARTED = "Started";
    private static final String STOPPED = "Stopped";
    private static final String RESUME = "Resume";

    private static final String TIMESTAMP_FIELD = "timestamp";

    private Events() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "Exception", // discriminator
     *     "timestamp" : "string",    // event timestamp
     *     "cause"     : "string",    // Exception type canonical name
     *     "message"   : "string"     // Exception comment message
     * }
     */

    public static WritableMap exception(Exception cause) {
        WritableMap event = createEvent(EXCEPTION);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return event;
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "UnsuccessfullyResponse", // discriminator
     *     "timestamp" : "string",                 // event timestamp
     *     "message"   : "string",                 // response message
     *     "code"      : "string"                  // response code
     * }
     */

    public static WritableMap unsuccessfullyResponse(int code, String message) {
        WritableMap event = createEvent(UNSUCCESSFULLY_RESPONSE);
        event.putString("message", message);
        event.putString("code", String.valueOf(code));
        return event;
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "Resume",  // discriminator
     *     "timestamp" : "string",  // event timestamp
     *     "duration"  : "string"   // duration from last fail
     * }
     */

    public static WritableMap resume(long durationFromLastFail) {
        WritableMap event = createEvent(RESUME);
        event.putString("duration", String.valueOf(durationFromLastFail));
        return acceptTimestamp(event);
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "Started",   // discriminator
     *     "timestamp" : "string"    // event timestamp
     * }
     */

    public static WritableMap started() {
        return createEvent(STARTED);
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "Stopped", // discriminator
     *     "timestamp" : "string",  // event timestamp
     *     "uptime"    : "string"   // uptime duration of the HeartbeatRunner
     * }
     */

    public static WritableMap stopped(long uptime) {
        WritableMap event = createEvent(STOPPED);
        event.putString("uptime", String.valueOf(uptime));
        return event;
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "StartFailed", // discriminator
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static WritableMap startFailed(Exception cause) {
        WritableMap event = createEvent(START_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return event;
    }


    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "StopFailed",  // discriminator
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static WritableMap stopFailed(Exception cause) {
        WritableMap event = createEvent(STOP_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return event;
    }
    
    private static WritableMap createEvent(String type) {
        WritableMap event = Arguments.createMap();
        event.putString(EVENT_TYPE, type);
        return acceptTimestamp(event);
    }

    private static WritableMap acceptTimestamp(WritableMap event) {
        event.putString(TIMESTAMP_FIELD, Date.now());
        return event;
    }
}
