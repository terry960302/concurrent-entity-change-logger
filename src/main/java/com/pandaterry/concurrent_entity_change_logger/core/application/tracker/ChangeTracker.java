package com.pandaterry.concurrent_entity_change_logger.core.application.tracker;

import com.pandaterry.concurrent_entity_change_logger.core.domain.Operation;

import java.util.Map;

public interface ChangeTracker {
    Map<String, String> trackChanges(Object oldEntity, Object newEntity);

    boolean supports(Operation operationType);
}