let _ = require('lodash');
let {NativeModules, DeviceEventEmitter} = require('react-native');
let SimpozioBackgroundWorker = NativeModules.SimpozioBackgroundWorker;

let listeners = {};
let currentHeartbeatMetadata = {};
let currentPingMetadata = {};
let isHeartbeatStarted = false;
let isPingStarted = false;
let META = '_simpozioListenerId';

let EVENT_START_FAILED = "startFailed";
let EVENT_STOP_FAILED = "stopFailed";
let EVENT_STARTED = "started";
let EVENT_STOPPED = "stopped";

let eventPromiseHelper = (eventSuccess, eventFailed, service) => {
    return new Promise((resolve, reject) => {
        let waitFor;
        let waitForFailed;

        waitFor = DeviceEventEmitter.addListener(eventSuccess, (event) => {
            if (event.service === service) {
                waitForFailed.remove();
                waitFor.remove();
                return resolve();
            }

        });

        waitForFailed = DeviceEventEmitter.addListener(eventFailed, (error) => {
            if (event.service === service) {
                waitForFailed.remove();
                waitFor.remove();
                return reject(error);
            }
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


let updateHeartbeatMetadata = (metadata) => {
    currentHeartbeatMetadata = _.assign({}, currentHeartbeatMetadata, {
        baseUrl: metadata.baseUrl,
        headers: _.assign({}, currentHeartbeatMetadata.headers, metadata.headers),
        requestBody: _.assign({}, currentHeartbeatMetadata.body, metadata.body)
    });

    return currentHeartbeatMetadata;
};

let updatePingMetadata = (metadata) => {
    currentPingMetadata = _.assign({}, currentPingMetadata, metadata);
    return currentPingMetadata;
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
        SimpozioBackgroundWorker.updateHeartbeat(updateHeartbeatMetadata(metadata));
        return Promise.resolve();
    } else {
        const eventPromise = eventPromiseHelper(EVENT_STARTED, EVENT_START_FAILED, 'heartbeat').then(() => {
            isHeartbeatStarted = true;
        });

        SimpozioBackgroundWorker.startHeartbeat(updateHeartbeatMetadata(metadata));

        return eventPromise;
    }
};

let startPing = (metadata) => {
    if (isHeartbeatStarted) {
        SimpozioBackgroundWorker.updateHeartbeat(updateHeartbeatMetadata(metadata));
        return Promise.resolve();
    } else {
        const eventPromise = eventPromiseHelper(EVENT_STARTED, EVENT_START_FAILED, 'ping').then(() => {
            isPingStarted = true;
        });

        SimpozioBackgroundWorker.startHeartbeat(updateHeartbeatMetadata(metadata));

        return eventPromise;
    }
};

let updateHeartbeat = (metadata) => {
    let data = updateHeartbeatMetadata(metadata);

    if (isHeartbeatStarted) {
        SimpozioBackgroundWorker.updateHeartbeat(data);
    }
};

let updatePing = (metadata) => {
    let data = updatePingMetadata(metadata);

    if (isPingStarted) {
        SimpozioBackgroundWorker.updatePing(data);
    }
};

let stopHeartbeat = () => {
    if (!isHeartbeatStarted) {
        return Promise.resolve();
    } else {
        const eventPromise = eventPromiseHelper(EVENT_STOPPED, EVENT_STOP_FAILED, 'heartbeat').then(() => {
            removeAllListeners();
            isHeartbeatStarted = false;
        });
        SimpozioBackgroundWorker.stopHeartbeat();
        return eventPromise;
    }
};

let stopPing = () => {
    if (!isHeartbeatStarted) {
        return Promise.resolve();
    } else {
        const eventPromise = eventPromiseHelper(EVENT_STOPPED, EVENT_STOP_FAILED, 'ping').then(() => {
            removeAllListeners();
            isPingStarted= false;
        });
        SimpozioBackgroundWorker.stopHeartbeat();
        return eventPromise;
    }
};

module.exports = {
    startHeartbeat,
    updateHeartbeat,
    stopHeartbeat,
    addListener,
    startPing,
    updatePing,
    stopPing,
    removeListener,
    removeAllListeners
};
