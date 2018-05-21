let _ = require('lodash');
let {NativeModules, DeviceEventEmitter} = require('react-native');
let SimpozioJavaService = NativeModules.SimpozioJavaService;

let listeners = {};
let currentMetadata = {};
let isHeartbeatStarted = false;
let META = '_simpozioListenerId';

//TODO take it from SimpozioJavaService
let TRACE_URL = "/signals/trace";
let HEARTBEAT_URL = "/signals/heartbeat";

let EVENT_HEARTBEAT_FAILED = "heartbeatFailed";
let EVENT_START_FAILED = "startFailed";
let EVENT_STOP_FAILED = "stopFailed";
let EVENT_UNKNOWN_URL = "unknownUrl";
let EVENT_EXCEPTION = "exception";
let EVENT_STARTED = "started";
let EVENT_STOPPED = "stopped";
let EVENT_RESUME = "resume";



let eventPromiseHelper = (eventSuccess, eventFailed) => {

    return new Promise((resolve, reject) => {
        let waitFor;
        let waitForFailed;

        waitFor = DeviceEventEmitter.addListener(eventSuccess, () => {
            waitForFailed.remove();
            waitFor.remove();
            return resolve();
        });

        waitForFailed = DeviceEventEmitter.addListener(eventFailed, (error) => {
            waitForFailed.remove();
            waitFor.remove();
            return reject(error);
        });
    });
};

let addListener = (event, cb) => {
    let key = getKey(cb);
    listeners[key] = DeviceEventEmitter.addListener(event,
        (body) => {
            cb(body);
        });
    return key;
};

let removeListener = (key) => {
    if (!listeners[key]) {
        return;
    }

    listeners[key].remove();
    listeners[key] = null;
};

let removeAllListeners = (key) => {
    if (_.isEmpty(listeners)) {
        return;
    }

    _.forEach(listeners, listener => {
        listener.remove();
        listeners[key] = null;
    });

    listeners = {};
};


let updateMetadata = (metadata) => {
    currentMetadata = _.assign({}, currentMetadata, {
        baseUrl: metadata.baseUrl,
        call: HEARTBEAT_URL,
        headers: _.assign({}, currentMetadata.headers, metadata.headers),
        requestBody: _.assign({}, currentMetadata.body, metadata.body)
    });

    return currentMetadata;
};

let getKey = (listener) => {
    if (!listener.hasOwnProperty(META)) {
        if (!Object.isExtensible(listener)) {
            return 'F';
        }

        Object.defineProperty(listener, META, {
            value: _.uniqueId('SIMPOZIO_LISTENER_'),
        });
    }

    return listener[META];
};

let startHeartbeat = (metadata) => {
    if (isHeartbeatStarted) {
        SimpozioJavaService.update(updateMetadata(metadata));
        return Promise.resolve();
    } else {
        const eventPromise = eventPromiseHelper(EVENT_STARTED, EVENT_START_FAILED).then(() => {
            isHeartbeatStarted = true;
        });

        SimpozioJavaService.start(updateMetadata(metadata));

        return eventPromise;
    }
};

let updateHeartbeat = (metadata) => {
    let data = updateMetadata(metadata);

    if (isHeartbeatStarted) {
        SimpozioJavaService.update(data);
    }
};

let stopHeartbeat = () => {
    if (!isHeartbeatStarted) {
        return Promise.resolve();
    } else {
        const eventPromise = eventPromiseHelper(EVENT_STOPPED, EVENT_STOP_FAILED).then(() => {
            removeAllListeners();
            isHeartbeatStarted = false;
        });
        SimpozioJavaService.stop(HEARTBEAT_URL);
        return eventPromise;
    }
};

module.exports = {
    startHeartbeat,
    updateHeartbeat,
    stopHeartbeat,
    addListener,
    removeListener,
    removeAllListeners
};
