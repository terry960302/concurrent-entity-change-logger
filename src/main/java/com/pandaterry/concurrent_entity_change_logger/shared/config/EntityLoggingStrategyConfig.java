package com.pandaterry.concurrent_entity_change_logger.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.mapper.LogEntryMapper;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.queue.BlockingQueueAdapter;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.storage.DiskLogWriteAdapter;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.LogStoragePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.QueuePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityLoggingStrategyConfig {
    @Bean
    public LogStoragePort logStoragePort(EntityLoggingProperties properties, LogEntryMapper logEntryMapper, ObjectMapper objectMapper) {
        return switch (properties.getStorageType().toLowerCase()) {
            case "disk" -> new DiskLogWriteAdapter(logEntryMapper, objectMapper);
            default -> throw new IllegalArgumentException(
                    "Unknown storage type: " + properties.getStorageType());
        };
    }

    @Bean
    public QueuePort queuePort(EntityLoggingProperties properties, LogEntryMapper logEntryMapper) {
        return switch (properties.getProcessType().toLowerCase()) {
            case "blocking" -> new BlockingQueueAdapter(properties, logEntryMapper);
            default -> throw new IllegalArgumentException(
                    "Unknown storage type: " + properties.getStorageType());
        };
    }
}