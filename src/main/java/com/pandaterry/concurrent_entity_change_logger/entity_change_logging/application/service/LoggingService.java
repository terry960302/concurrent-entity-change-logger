package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.service;

import com.pandaterry.concurrent_entity_change_logger.shared.config.EntityLoggingProperties;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.LogStoragePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.MetricsPort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.QueuePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.service.tracker.EntityChangeTracker;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.ChangeSet;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.EntityIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoggingService {
    private final LogStoragePort logStoragePort;
    private final QueuePort queuePort;
    private final MetricsPort metricsPort;
    private final EntityChangeTracker changeTracker;
    private final EntityLoggingProperties loggingProperties;

    public void logChange(Object oldEntity, Object newEntity, Operation operation) {
        if (!loggingProperties.shouldLogChanges(oldEntity != null ? oldEntity : newEntity)) {
            return;
        }

        LogEntry entry = createLogEntry(oldEntity, newEntity, operation);

        try {
            logStoragePort.write(entry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        offerToQueue(entry);
    }

    private LogEntry createLogEntry(Object oldEntity, Object newEntity, Operation operation) {
        Map<String, Object> changes = changeTracker.trackChanges(oldEntity, newEntity, operation);

        EntityIdentifier entityIdentifier = EntityIdentifier.fromEntity(
                oldEntity != null ? oldEntity : newEntity
        );
        ChangeSet changeSet = ChangeSet.from(changes);

        return LogEntry.create(entityIdentifier, changeSet, operation);
    }

    private void offerToQueue(LogEntry entry) {
        long startNanos = System.nanoTime();
        long tookNanos = System.nanoTime() - startNanos;

        if (queuePort.remainingCapacity() == 0) {
            return;
        }

        boolean offered = queuePort.offer(entry);
        metricsPort.recordOfferLatency(tookNanos);

        if (!offered) {
            metricsPort.incrementDroppedCount();
        }

        metricsPort.gaugeQueueSize(queuePort.size());
    }
}
