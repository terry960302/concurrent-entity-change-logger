package com.pandaterry.concurrent_entity_change_logger.core.application.strategy;

import com.pandaterry.concurrent_entity_change_logger.core.domain.Operation;

public interface LoggingStrategy {
    void logChange(Object oldEntity, Object newEntity, Operation operation);

    void shutdown();

    int flush();
}