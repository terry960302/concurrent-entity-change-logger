package com.pandaterry.concurrent_entity_change_logger.core.config;

import com.pandaterry.concurrent_entity_change_logger.core.factory.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.repository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.strategy.BlockingQueueLoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.strategy.LoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.tracker.EntityChangeTracker;
import com.pandaterry.concurrent_entity_change_logger.core.util.EntityLoggingCondition;

import jakarta.persistence.EntityManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityLoggingStrategyConfig {
    @Bean
    public LoggingStrategy loggingStrategy(
            LogEntryRepository logEntryRepository,
            EntityManager entityManager,
            EntityChangeTracker changeTracker,
            EntityLoggingCondition entityLoggingCondition,
            LogEntryFactory logEntryFactory) {
        return new BlockingQueueLoggingStrategy(logEntryRepository, entityManager, changeTracker, entityLoggingCondition, logEntryFactory);
    }
}