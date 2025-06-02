package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage;

import com.pandaterry.concurrent_entity_change_logger.core.domain.entity.LogEntry;
import java.io.IOException;

public interface LogStorage {
    void init() throws IOException;

    void write(LogEntry entry) throws IOException;

    void close();
}