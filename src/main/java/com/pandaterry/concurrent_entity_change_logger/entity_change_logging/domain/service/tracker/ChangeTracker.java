package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.service.tracker;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;

import java.util.Map;

public interface ChangeTracker {
    Map<String, Object> trackChanges(Object oldEntity, Object newEntity);

    boolean supports(Operation operationType);
}