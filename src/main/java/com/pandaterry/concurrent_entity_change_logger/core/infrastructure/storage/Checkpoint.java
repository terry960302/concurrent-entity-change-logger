package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage;

import java.time.LocalDateTime;

public class Checkpoint {
    private final long position;
    private final LocalDateTime timestamp;

    public Checkpoint(long position, LocalDateTime timestamp) {
        this.position = position;
        this.timestamp = timestamp;
    }

    public long getPosition() {
        return position;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}