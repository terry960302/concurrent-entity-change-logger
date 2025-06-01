package com.pandaterry.concurrent_entity_change_logger.core.application.tracker;

import com.pandaterry.concurrent_entity_change_logger.core.domain.enumerated.OperationType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EntityChangeTracker {
    private final List<ChangeTracker> trackers;

    public EntityChangeTracker(List<ChangeTracker> trackers) {
        this.trackers = trackers;
    }

    public Map<String, String> trackChanges(Object oldEntity, Object newEntity, OperationType operation) {
        return trackers.stream()
                .filter(tracker -> tracker.supports(operation))
                .findFirst()
                .map(tracker -> tracker.trackChanges(oldEntity, newEntity))
                .orElse(new HashMap<>());
    }
}