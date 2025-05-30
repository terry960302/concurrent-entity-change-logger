package com.pandaterry.concurrent_entity_change_logger.core.config;

import com.pandaterry.concurrent_entity_change_logger.core.repository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.strategy.BlockingQueueLoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.strategy.LoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.monitoring.service.EntityChangeMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingStrategyConfig {
    @Bean
    public LoggingStrategy loggingStrategy(
            LogEntryRepository logEntryRepository) {
        return new BlockingQueueLoggingStrategy(logEntryRepository);
    }
}