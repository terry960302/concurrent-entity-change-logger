package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo;

import lombok.Value;

import java.util.Objects;
import java.util.UUID;

@Value
public class LogEntryId {
    private final UUID value;

    private LogEntryId(UUID value) {
        this.value = Objects.requireNonNull(value, "LogEntry ID cannot be null");
    }

    public static LogEntryId generate() {
        return new LogEntryId(UUID.randomUUID());
    }

    public static LogEntryId of(UUID value) {
        return new LogEntryId(value);
    }

    public UUID getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogEntryId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value.toString(); }
}
