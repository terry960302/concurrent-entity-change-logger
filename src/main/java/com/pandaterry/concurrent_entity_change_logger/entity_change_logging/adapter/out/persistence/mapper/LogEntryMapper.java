package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.mapper;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.ChangeSet;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.EntityIdentifier;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.LogEntryId;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.SubmissionContext;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.entity.LogEntryJpo;
import com.pandaterry.concurrent_entity_change_logger.shared.infrastructure.mapper.BaseMapper;
import org.springframework.stereotype.Component;

@Component
public class LogEntryMapper extends BaseMapper<LogEntry, LogEntryJpo> {
    @Override
    public LogEntry toDomain(LogEntryJpo entity) {
        return LogEntry.create(
                LogEntryId.of(entity.getId()),
                EntityIdentifier.of(entity.getEntityName(), entity.getEntityId()),
                ChangeSet.from(entity.getChanges()),
                entity.getOperation(),
                entity.getRecordedAt(),
                SubmissionContext.from(entity.getContext())
        );
    }

    @Override
    public LogEntryJpo toEntity(LogEntry logEntry) {
        return LogEntryJpo.builder()
                .id(logEntry.getId().getValue())
                .entityId(logEntry.getEntityIdentifier().getEntityId())
                .entityName(logEntry.getEntityIdentifier().getEntityName())
                .changes(logEntry.getChangeSet().toMap())
                .context(logEntry.getContext().toMap())
                .recordedAt(logEntry.getRecordedAt())
                .operation(logEntry.getOperation())
                .build();
    }
}
