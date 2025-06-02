package com.pandaterry.concurrent_entity_change_logger.core.application.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pandaterry.concurrent_entity_change_logger.core.domain.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.domain.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.factory.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.respository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage.LogStorage;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.config.EntityLoggingProperties;
import com.pandaterry.concurrent_entity_change_logger.monitoring.annotation.LoggingMetrics;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class BlockingQueueLoggingStrategy implements LoggingStrategy {
    private final LogEntryRepository logEntryRepository;
    private final EntityLoggingProperties loggingProperties;
    private final LogEntryFactory logEntryFactory;
    private final ObjectMapper objectMapper;
    private final LogStorage logStorage;

    private BlockingQueue<LogEntry> logQueue;
    private ExecutorService logProcessorPool;

    public BlockingQueueLoggingStrategy(LogEntryRepository logEntryRepository,
            EntityLoggingProperties loggingProperties,
            LogEntryFactory logEntryFactory,
            LogStorage logStorage) {
        this.logEntryRepository = logEntryRepository;
        this.loggingProperties = loggingProperties;
        this.logEntryFactory = logEntryFactory;
        this.logStorage = logStorage;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() throws IOException {
        logStorage.init();
        this.logQueue = new LinkedBlockingQueue<>(loggingProperties.getStrategy().getQueueSize());
        this.logProcessorPool = Executors.newFixedThreadPool(loggingProperties.getStrategy().getThreadPoolSize());

        for (int i = 0; i < loggingProperties.getStrategy().getThreadPoolSize(); i++) {
            logProcessorPool.submit(this::processLogs);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    @LoggingMetrics({ LoggingMetrics.MetricType.PROCESSING_TIME, LoggingMetrics.MetricType.ERROR_COUNT })
    public void logChange(Object oldEntity, Object newEntity, OperationType operation) {
        if (!loggingProperties.shouldLogChanges(oldEntity != null ? oldEntity : newEntity)) {
            return;
        }
        LogEntry entry = logEntryFactory.create(oldEntity, newEntity, operation);
        try {
            logStorage.write(entry);
            offerToQueue(entry);
        } catch (IOException e) {
            log.error("Failed to save log entry to disk", e);
        }
    }

    @LoggingMetrics({ LoggingMetrics.MetricType.QUEUE_SIZE, LoggingMetrics.MetricType.PROCESSED_COUNT,
            LoggingMetrics.MetricType.ERROR_COUNT })
    private void offerToQueue(LogEntry entry) {
        if (logQueue.remainingCapacity() == 0)
            return;
        logQueue.offer(entry);
    }

    private void processLogs() {
        while (!Thread.currentThread().isInterrupted()) {
            List<LogEntry> batch = new ArrayList<>(loggingProperties.getJpaBatchSize());
            int drained = logQueue.drainTo(batch, loggingProperties.getJpaBatchSize());
            if (drained > 0) {
                saveBatch(batch);
            }
        }
    }

    @LoggingMetrics({ LoggingMetrics.MetricType.QUEUE_SIZE, LoggingMetrics.MetricType.PROCESSED_COUNT,
            LoggingMetrics.MetricType.ERROR_COUNT })
    private void saveBatch(List<LogEntry> toSave) {
        for (LogEntry entry : toSave) {
            try {
                String changesJson = objectMapper.writeValueAsString(entry.getChanges());
                logEntryRepository.batchInsert(
                        entry.getEntityName(),
                        entry.getEntityId(),
                        entry.getOperation(),
                        changesJson);
            } catch (Exception ignored) {
            }
        }
    }

    @Scheduled(fixedDelayString = "${logging.strategy.flush-interval:5000}")
    @Override
    public void flush() {
        List<LogEntry> batch = new ArrayList<>(loggingProperties.getJpaBatchSize());
        logQueue.drainTo(batch, loggingProperties.getJpaBatchSize());
        if (!batch.isEmpty()) {
            saveBatch(batch);
        }
    }

    @Override
    public void shutdown() {
        logProcessorPool.shutdown();
        try {
            if (!logProcessorPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logProcessorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            logProcessorPool.shutdownNow();
        }
        flush();
        logStorage.close();
    }
}