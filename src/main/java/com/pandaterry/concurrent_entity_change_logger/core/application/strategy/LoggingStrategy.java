package com.pandaterry.concurrent_entity_change_logger.core.application.strategy;

import com.pandaterry.concurrent_entity_change_logger.core.domain.enumerated.OperationType;

public interface LoggingStrategy {
    void logChange(Object oldEntity, Object newEntity, OperationType operation);

    void shutdown();

    void flush();
}