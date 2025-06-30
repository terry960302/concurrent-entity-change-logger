package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.ChangeSet;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.EntityIdentifier;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.LogEntryId;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.SubmissionContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LogEntry {
    @EqualsAndHashCode.Include
    private final LogEntryId id;
    private final EntityIdentifier entityIdentifier;
    private final ChangeSet changeSet;
    private final Operation operation;
    private final Instant recordedAt;
    private final SubmissionContext context;

    private LogEntry(LogEntryId id, EntityIdentifier entityIdentifier,
                     ChangeSet changeSet, Operation operation,
                     Instant recordedAt, SubmissionContext context) {
        this.id = id;
        this.entityIdentifier = entityIdentifier;
        this.changeSet = changeSet;
        this.operation = operation;
        this.recordedAt = recordedAt;
        this.context = context;
    }

    public static LogEntry create(EntityIdentifier entityIdentifier,
                                  ChangeSet changeSet,
                                  Operation operation) {
        validateBusinessRules(entityIdentifier, changeSet, operation);

        return new LogEntry(
                LogEntryId.generate(),
                entityIdentifier,
                changeSet,
                operation,
                Instant.now(),
                SubmissionContext.current()
        );
    }

    public static LogEntry create(
            LogEntryId logEntryId,
            EntityIdentifier entityIdentifier,
            ChangeSet changeSet,
            Operation operation,
            Instant recordedAt,
            SubmissionContext context) {
        validateBusinessRules(entityIdentifier, changeSet, operation);

        return new LogEntry(
                logEntryId,
                entityIdentifier,
                changeSet,
                operation,
                recordedAt,
                context
        );
    }

    // 비즈니스 로직 - Rich Domain Model의 핵심
    public boolean isSignificantChange() {
        return switch (operation) {
            case CREATE -> changeSet.hasAnyChanges();
            case UPDATE -> changeSet.hasBusinessImpactingChanges();
            case DELETE -> true; // DELETE는 항상 중요
        };
    }

    public boolean requiresHighPriorityProcessing() {
        return changeSet.containsCriticalFields() ||
                operation == Operation.DELETE;
    }

    public LogEntry enrichWithContext(Map<String, Object> additionalContext) {
        if (additionalContext.isEmpty()) {
            return this;
        }

        ChangeSet enrichedChangeSet = changeSet.enrichWith(additionalContext);
        return new LogEntry(id, entityIdentifier, enrichedChangeSet,
                operation, recordedAt, context);
    }

    public boolean canBeGroupedWith(LogEntry other) {
        return this.entityIdentifier.equals(other.entityIdentifier) &&
                this.operation == other.operation &&
                Duration.between(this.recordedAt, other.recordedAt)
                        .abs().toMinutes() < 5;
    }

    private static void validateBusinessRules(EntityIdentifier entityIdentifier,
                                              ChangeSet changeSet,
                                              Operation operation) {
        Objects.requireNonNull(entityIdentifier, "Entity identifier is mandatory");
        Objects.requireNonNull(changeSet, "Change set is mandatory");
        Objects.requireNonNull(operation, "Operation is mandatory");

        if (operation == Operation.UPDATE && !changeSet.hasAnyChanges()) {
//            throw new LOgException("UPDATE operation must have changes");
        }

        if (changeSet.exceedsMaximumSize()) {
//            throw new InvalidLogEntryException("Too many changes in single entry");
        }
    }
}
