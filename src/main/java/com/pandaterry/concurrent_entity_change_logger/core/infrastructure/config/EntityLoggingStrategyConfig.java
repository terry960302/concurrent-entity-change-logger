package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.persistence.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.persistence.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.application.strategy.BlockingQueueLoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.application.strategy.LoggingStrategy;

import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage.DiskLogWriter;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage.LogStorage;
import com.pandaterry.concurrent_entity_change_logger.monitoring.service.MicrometerLogMetricsRecorder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityLoggingStrategyConfig {
    @Bean
    public LogStorage logStorage(EntityLoggingProperties properties, ObjectMapper objectMapper) {
        return switch (properties.getStorageType().toLowerCase()) {
            case "disk" -> new DiskLogWriter(objectMapper);
            default -> throw new IllegalArgumentException(
                    "Unknown storage type: " + properties.getStorageType());
        };
    }

    @Bean
    public LoggingStrategy loggingStrategy(
            LogEntryRepository logEntryRepository,
            EntityLoggingProperties loggingProperties,
            LogEntryFactory logEntryFactory,
            LogStorage logStorage,
            ObjectMapper objectMapper,
            MicrometerLogMetricsRecorder metricsRecorder) {
        return new BlockingQueueLoggingStrategy(logEntryRepository, loggingProperties, logEntryFactory, logStorage, objectMapper, metricsRecorder);
    }
}