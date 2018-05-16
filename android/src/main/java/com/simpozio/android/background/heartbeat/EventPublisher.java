package com.simpozio.android.background.heartbeat;

import android.os.Bundle;

public interface EventPublisher {
    void fireEvent(Bundle event);
}
