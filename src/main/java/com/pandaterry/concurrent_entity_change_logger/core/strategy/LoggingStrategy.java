package com.pandaterry.concurrent_entity_change_logger.core.strategy;

import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.enumerated.OperationType;

public interface LoggingStrategy {
    void logChange(Object oldEntity, Object newEntity, OperationType operation);

    void shutdown();

    void flush();
}