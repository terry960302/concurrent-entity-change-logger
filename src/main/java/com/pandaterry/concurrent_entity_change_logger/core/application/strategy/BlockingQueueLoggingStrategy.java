package com.pandaterry.concurrent_entity_change_logger.core.application.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pandaterry.concurrent_entity_change_logger.core.domain.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.domain.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.factory.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.respository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.shared.config.EntityLoggingProperties;
import com.pandaterry.concurrent_entity_change_logger.monitoring.annotation.LoggingMetrics;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class BlockingQueueLoggingStrategy implements LoggingStrategy {
    private final LogEntryRepository logEntryRepository;
    private final EntityLoggingProperties loggingProperties;
    private final LogEntryFactory logEntryFactory;
    private final ObjectMapper objectMapper;

    private BlockingQueue<LogEntry> logQueue;
    private ExecutorService logProcessorPool;
    private ConcurrentLinkedQueue<LogEntry> batchQueue;

    public BlockingQueueLoggingStrategy(LogEntryRepository logEntryRepository,
            EntityLoggingProperties loggingProperties,
            LogEntryFactory logEntryFactory) {
        this.logEntryRepository = logEntryRepository;
        this.loggingProperties = loggingProperties;
        this.logEntryFactory = logEntryFactory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        this.logQueue = new LinkedBlockingQueue<>(loggingProperties.getStrategy().getQueueSize());
        this.batchQueue = new ConcurrentLinkedQueue<>();
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
        offerToQueue(entry);
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
            try {
                LogEntry entry = pollFromQueue();
                if (entry != null) {
                    processLogEntry(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private LogEntry pollFromQueue() throws InterruptedException {
        return logQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    @LoggingMetrics({ LoggingMetrics.MetricType.QUEUE_SIZE, LoggingMetrics.MetricType.BATCH_QUEUE_SIZE,
            LoggingMetrics.MetricType.PROCESSED_COUNT, LoggingMetrics.MetricType.ERROR_COUNT })
    private void processLogEntry(LogEntry entry) {
        if (entry == null)
            return;
        batchQueue.add(entry);
        if (batchQueue.size() >= loggingProperties.getJpaBatchSize()) {
            flushBatch();
        }
    }

    @Transactional
    @LoggingMetrics({ LoggingMetrics.MetricType.BATCH_QUEUE_SIZE, LoggingMetrics.MetricType.ERROR_COUNT })
    private void flushBatch() {
        List<LogEntry> toSave = collectBatchEntries();
        if (!toSave.isEmpty()) {
            saveBatch(toSave);
        }
    }

    private List<LogEntry> collectBatchEntries() {
        List<LogEntry> toSave = new ArrayList<>();
        LogEntry entry;
        while ((entry = batchQueue.poll()) != null) {
            toSave.add(entry);
            if (toSave.size() >= loggingProperties.getJpaBatchSize()) {
                break;
            }
        }
        return toSave;
    }

    @LoggingMetrics({ LoggingMetrics.MetricType.BATCH_QUEUE_SIZE, LoggingMetrics.MetricType.ERROR_COUNT })
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
        flushBatch();
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
    }
}