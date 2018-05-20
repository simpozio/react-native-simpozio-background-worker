let _ = require('lodash');
let {NativeModules, DeviceEventEmitter} =  require('react-native');
let SimpozioJavaService = NativeModules.SimpozioJavaService;

let listeners = {};
let currentMetadata = {};
let isHeartbeatStarted = false;
let META = '_simpozioListenerId';

//TODO take it from SimpozioJavaService
let TRACE_URL = "/signals/trace";
let HEARTBEAT_URL = "/signals/heartbeat";

let EVENT_UNSUCCESSFULLY_RESPONSE = "unsuccessfullyResponse";
let EVENT_START_FAILED = "startFailed";
let EVENT_STOP_FAILED = "stopFailed";
let EVENT_UNKNOWN_URL = "unknownUrl";
let EVENT_EXCEPTION = "exception";
let EVENT_STARTED = "started";
let EVENT_STOPPED = "stopped";
let EVENT_RESUME = 'resume';


let eventPromiseHelper = (eventSuccess, eventFailed) => {

    return new Promise((resolve, reject) => {
        let waitFor;
        let waitForFailed;
        let waitForException;

        waitFor = DeviceEventEmitter.addListener(eventSuccess, () => {
            waitForFailed.remove();
            waitFor.remove();
            waitForException.remove();
            return resolve();
        });

        waitForFailed = DeviceEventEmitter.addListener(eventFailed, (error) => {
            waitForFailed.remove();
            waitFor.remove();
            waitForException.remove();
            return reject(error);
        });

        waitForException = DeviceEventEmitter.addListener(exception, (error) => {
            waitForFailed.remove();
            waitForException.remove();
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
        baseUrl: currentMetadata.baseUrl,
        call: HEARTBEAT_URL,
        headers: _.assign({}, currentMetadata.headers, metadata.headers),
        body: _.assign({}, currentMetadata.body, metadata.body),
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
        SimpozioJavaService.start(updateMetadata(metadata));
        return eventPromiseHelper(EVENT_STARTED, EVENT_START_FAILED).then(() => {
            isHeartbeatStarted = true;
        });
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
        SimpozioJavaService.stop();
        return eventPromiseHelper(EVENT_STOPPED, EVENT_STOP_FAILED).then(() => {
            removeAllListeners();
            isHeartbeatStarted = false;
        });
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