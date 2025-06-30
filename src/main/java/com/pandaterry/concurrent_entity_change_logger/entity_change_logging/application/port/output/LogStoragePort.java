package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;

import java.io.IOException;

public interface LogStoragePort {
    void write(LogEntry entry) throws IOException;
    void init() throws IOException;
    void close();
}