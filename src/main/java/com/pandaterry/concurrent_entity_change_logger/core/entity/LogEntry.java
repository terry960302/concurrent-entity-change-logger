package com.pandaterry.concurrent_entity_change_logger.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Map;
import com.pandaterry.concurrent_entity_change_logger.core.annotation.ExcludeFromLogging;

@Entity
@Table(name = "log_entry_tb")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ExcludeFromLogging
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String entityName;
    @Column
    private String entityId;

    @ElementCollection
    @CollectionTable(name = "log_entry_changes", joinColumns = @JoinColumn(name = "log_entry_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "change_value")
    private Map<String, String> changes;

    @Column
    private String operation;

    @Override
    public String toString() {
        return String.format("LogEntry{entityName='%s', entityId='%s', operation='%s', changes=%s}",
                entityName, entityId, operation, changes);
    }
}