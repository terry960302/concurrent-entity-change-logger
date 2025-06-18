package com.pandaterry.concurrent_entity_change_logger.core.domain;

import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.persistence.MapToJsonConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.pandaterry.concurrent_entity_change_logger.core.common.annotation.ExcludeFromLogging;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "log_entries")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ExcludeFromLogging
public class LogEntry {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column
    private String entityName;
    @Column
    private String entityId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> changes;

    @Enumerated(EnumType.STRING)
    @Column
    private Operation operation;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}