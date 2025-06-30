package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;

import java.util.List;

public interface BatchPersistencePort {
    void saveBatch(List<LogEntry> batch);
}