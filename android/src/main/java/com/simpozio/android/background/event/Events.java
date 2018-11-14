package com.simpozio.android.background.event;

import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.simpozio.android.background.heartbeat.DateFormatted;

import org.joda.time.DateTime;

public final class Events {

    public static final String EVENT_TYPE = "type";

    private static final String HEARTBEAT_FAILED = "heartbeatFailed";
    private static final String PING_FAILED = "pingFailed";
    private static final String SERVER_TIMESTAMP = "serverTimestamp";
    private static final String START_FAILED = "startFailed";
    private static final String STOP_FAILED = "stopFailed";
    private static final String EXCEPTION = "exception";
    private static final String STARTED = "started";
    private static final String STOPPED = "stopped";
    private static final String RESUME = "resume";
    private static final String DEVIATION = "deviation";

    private static final String SERVICE = "service";

    private static final String PING = "ping";
    private static final String HEARTBEAT = "heartbeat";
    private static final String TRACE = "trace";

    private static final String TIMESTAMP_FIELD = "timestamp";

    private Events() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "exception", // discriminator
     *     "service"   : "ping",      // service name
     *     "timestamp" : "string",    // event timestamp
     *     "cause"     : "string",    // Exception type canonical name
     *     "message"   : "string"     // Exception comment message
     * }
     */

    public static Bundle pingException(Exception cause) {
        Bundle event = createEvent(EXCEPTION);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptPingService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "exception", // discriminator
     *     "service"   : "heartbeat",      // service name
     *     "timestamp" : "string",    // event timestamp
     *     "cause"     : "string",    // Exception type canonical name
     *     "message"   : "string"     // Exception comment message
     * }
     */

    public static Bundle heartbeatException(Exception cause) {
        Bundle event = createEvent(EXCEPTION);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptHeartbeatService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "exception", // discriminator
     *     "service"   : "trace",      // service name
     *     "timestamp" : "string",    // event timestamp
     *     "cause"     : "string",    // Exception type canonical name
     *     "message"   : "string"     // Exception comment message
     * }
     */

    public static Bundle traceException(Exception cause) {
        Bundle event = createEvent(EXCEPTION);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptTraceService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "resume",  // discriminator
     *     "service"   : "ping",  // service name (heartbeat, ping, trace)
     *     "timestamp" : "string",  // event timestamp
     *     "duration"  : "string"   // duration from last fail
     * }
     */

    public static Bundle pingResume(long durationFromLastFail) {
        Bundle event = createEvent(RESUME);
        event.putString("duration", String.valueOf(durationFromLastFail));
        return acceptPingService(acceptTimestamp(event));
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "resume",  // discriminator
     *     "service"   : "heartbeat",  // service name (heartbeat, ping, trace)
     *     "timestamp" : "string",  // event timestamp
     *     "duration"  : "string"   // duration from last fail
     * }
     */

    public static Bundle heartbeatResume(long durationFromLastFail) {
        Bundle event = createEvent(RESUME);
        event.putString("duration", String.valueOf(durationFromLastFail));
        return acceptHeartbeatService(acceptTimestamp(event));
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "resume",  // discriminator
     *     "service"   : "trace",  // service name (heartbeat, ping, trace)
     *     "timestamp" : "string",  // event timestamp
     *     "duration"  : "string"   // duration from last fail
     * }
     */

    public static Bundle traceResume(long durationFromLastFail) {
        Bundle event = createEvent(RESUME);
        event.putString("duration", String.valueOf(durationFromLastFail));
        return acceptTraceService(acceptTimestamp(event));
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"             : "serverTimestamp",  // discriminator
     *     "timestamp"        : "string",           // event timestamp
     *     "deviation"        : "string",           // server timestamp deviation
     *     "serverTimestamp"  : "string"            // timestamp from server with next format: yyyy-MM-dd'T'HH:mm:ss.SSSZ
     * }
     */

    public static Bundle serverTimestamp(DateTime timestamp, long deviationMillis) {
        Bundle event = createEvent(SERVER_TIMESTAMP);
        event.putString(DEVIATION, String.valueOf(deviationMillis));
        event.putString(SERVER_TIMESTAMP, timestamp.toString());
        return acceptTimestamp(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "started",   // discriminator
     *     "service"   : "ping",      // service name
     *     "timestamp" : "string"     // event timestamp
     * }
     */

    public static Bundle pingStarted() {
        return acceptPingService(createEvent(STARTED));
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "started",   // discriminator
     *     "service"   : "heartbeat",    // service name
     *     "timestamp" : "string"     // event timestamp
     * }
     */

    public static Bundle heartbeatStarted() {
        return acceptHeartbeatService(createEvent(STARTED));
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "started",   // discriminator
     *     "service"   : "trace",    // service name
     *     "timestamp" : "string"     // event timestamp
     * }
     */

    public static Bundle traceStarted() {
        return acceptTraceService(createEvent(STARTED));
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopped", // discriminator
     *     "service"   : "ping",    // service name
     *     "timestamp" : "string",  // event timestamp
     *     "uptime"    : "string"   // uptime duration of the HeartbeatRunner
     * }
     */

    public static Bundle pingStopped(long uptime) {
        Bundle event = createEvent(STOPPED);
        event.putString("uptime", String.valueOf(uptime));
        return acceptPingService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopped", // discriminator
     *     "service"   : "heartbeat",  // service name
     *     "timestamp" : "string",  // event timestamp
     *     "uptime"    : "string"   // uptime duration of the HeartbeatRunner
     * }
     */

    public static Bundle heartbeatStopped(long uptime) {
        Bundle event = createEvent(STOPPED);
        event.putString("uptime", String.valueOf(uptime));
        return acceptHeartbeatService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopped", // discriminator
     *     "service"   : "trace",  // service name
     *     "timestamp" : "string",  // event timestamp
     *     "uptime"    : "string"   // uptime duration of the HeartbeatRunner
     * }
     */

    public static Bundle traceStopped(long uptime) {
        Bundle event = createEvent(STOPPED);
        event.putString("uptime", String.valueOf(uptime));
        return acceptTraceService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "startFailed", // discriminator
     *     "service"   : "ping",      // service name
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle pingStartFailed(Exception cause) {
        Bundle event = createEvent(START_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptPingService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "startFailed", // discriminator
     *     "service"   : "heartbeat",      // service name
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle heartbeatStartFailed(Exception cause) {
        Bundle event = createEvent(START_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptHeartbeatService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "startFailed", // discriminator
     *     "service"   : "trace",      // service name
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle traceStartFailed(Exception cause) {
        Bundle event = createEvent(START_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptTraceService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopFailed",  // discriminator
     *     "service"   : "ping",      // service name
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle pingStopFailed(Exception cause) {
        Bundle event = createEvent(STOP_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptPingService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopFailed",  // discriminator
     *     "service"   : "heartbeat",      // service name
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle heartbeatStopFailed(Exception cause) {
        Bundle event = createEvent(STOP_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptHeartbeatService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "stopFailed",  // discriminator
     *     "service"   : "trace",      // service name
     *     "timestamp" : "string",      // event timestamp
     *     "cause"     : "string",      // Exception type canonical name
     *     "message"   : "string"       // Exception comment message
     * }
     */

    public static Bundle traceStopFailed(Exception cause) {
        Bundle event = createEvent(STOP_FAILED);
        event.putString("cause", cause.getClass().getCanonicalName());
        event.putString("message", cause.getMessage());
        return acceptTraceService(event);
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "heartbeatFailed", // discriminator
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

    public static Bundle heartbeatFailed(Throwable cause) {
        Bundle event = createEvent(HEARTBEAT_FAILED);
        event.putString("code", "-1");
        event.putString("message", cause.getMessage());
        event.putString("cause", cause.getClass().getCanonicalName());
        return event;
    }

    /**
     * @return event-object with next structure:
     * {
     *     "type"      : "pingFailed", // discriminator
     *     "timestamp" : "string",                 // event timestamp
     *     "message"   : "string",                 // response message or exception comment message
     *     "code"      : "string",                 // response code or -1
     *     "cause"     : "string"                  // Exception type canonical name or empty string
     * }
     */

    public static Bundle pingFailed(Throwable cause) {
        Bundle event = createEvent(PING_FAILED);
        event.putString("code", "-1");
        event.putString("message", cause.getMessage());
        event.putString("cause", cause.getClass().getCanonicalName());
        return event;
    }

    public static Bundle pingFailed(int code, String message) {
        Bundle event = createEvent(PING_FAILED);
        event.putString("code", String.valueOf(code));
        event.putString("message", message);
        event.putString("cause", "");
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

    private static Bundle acceptPingService(Bundle event) {
        event.putString(SERVICE, PING);
        return event;
    }

    private static Bundle acceptHeartbeatService(Bundle event) {
        event.putString(SERVICE, HEARTBEAT);
        return event;
    }

    private static Bundle acceptTraceService(Bundle event) {
        event.putString(SERVICE, TRACE);
        return event;
    }
}
