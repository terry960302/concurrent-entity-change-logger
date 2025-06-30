package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.entity;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.converter.ChangeSetJsonConverter;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.converter.ContextJsonConverter;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@Entity
@Table(name = "log_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LogEntryJpo {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "entity_id")
    private String entityId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ChangeSetJsonConverter.class)
    private Map<String, Object> changes;

    @Enumerated(EnumType.STRING)
    private Operation operation;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ContextJsonConverter.class)
    private Map<String, Object> context;
}