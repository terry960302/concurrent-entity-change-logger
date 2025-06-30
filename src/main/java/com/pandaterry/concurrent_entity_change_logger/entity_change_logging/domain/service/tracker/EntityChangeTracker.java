package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.service.tracker;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
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

    public Map<String, Object> trackChanges(Object oldEntity, Object newEntity, Operation operation) {
        return trackers.stream()
                .filter(tracker -> tracker.supports(operation))
                .findFirst()
                .map(tracker -> tracker.trackChanges(oldEntity, newEntity))
                .orElse(new HashMap<>());
    }
}