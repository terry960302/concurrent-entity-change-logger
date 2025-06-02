package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.config;

import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.factory.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.respository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.application.strategy.BlockingQueueLoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.application.strategy.LoggingStrategy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityLoggingStrategyConfig {
    @Bean
    public LoggingStrategy loggingStrategy(
            LogEntryRepository logEntryRepository,
            EntityLoggingProperties loggingProperties,
            LogEntryFactory logEntryFactory) {
        return new BlockingQueueLoggingStrategy(logEntryRepository, loggingProperties, logEntryFactory);
    }
}