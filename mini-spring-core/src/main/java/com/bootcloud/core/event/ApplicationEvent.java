package com.bootcloud.core.event;

import java.time.Clock;
import java.time.Instant;

public abstract class ApplicationEvent {
    private final Instant timestamp;
    private final Object source;

    public ApplicationEvent(Object source) {
        this.source = source;
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Object getSource() {
        return source;
    }
}
