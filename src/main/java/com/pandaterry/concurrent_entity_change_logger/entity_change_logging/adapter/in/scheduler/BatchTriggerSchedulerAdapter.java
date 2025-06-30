package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.in.scheduler;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.service.BatchProcessingService;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.shared.config.EntityLoggingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BatchTriggerSchedulerAdapter {
    private final BatchProcessingService batchProcessingService;

    @Scheduled(fixedDelayString = "${entity-logging.strategy.flush-interval:5000}")
    public void saveBatch(List<LogEntry> batch) {
        batchProcessingService.flush();
    }
}
