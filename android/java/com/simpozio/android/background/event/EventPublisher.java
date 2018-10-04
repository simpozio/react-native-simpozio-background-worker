package com.simpozio.android.background.event;

import android.os.Bundle;

public interface EventPublisher {
    void fireEvent(Bundle event);
}
