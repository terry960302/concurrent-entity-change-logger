package com.pandaterry.concurrent_entity_change_logger.core.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.repository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.monitoring.annotation.LoggingMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class BlockingQueueLoggingStrategy implements LoggingStrategy {
    private final BlockingQueue<LogEntry> logQueue;
    private final ExecutorService logProcessorPool;
    private final ConcurrentLinkedQueue<LogEntry> batchQueue;
    private final int batchSize;
    private final LogEntryRepository logEntryRepository;
    private final ObjectMapper objectMapper;

    public BlockingQueueLoggingStrategy(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
        this.logQueue = new LinkedBlockingQueue<>(100000);
        this.batchQueue = new ConcurrentLinkedQueue<>();
        this.batchSize = 10;
        this.logProcessorPool = Executors.newFixedThreadPool(5);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        for (int i = 0; i < 5; i++) {
            logProcessorPool.submit(this::processLogs);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    @LoggingMetrics({ LoggingMetrics.MetricType.PROCESSING_TIME, LoggingMetrics.MetricType.ERROR_COUNT })
    public void logChange(Object oldEntity, Object newEntity, OperationType operation) {
        try {
            LogEntry entry = createLogEntry(oldEntity, newEntity, operation);
            offerToQueue(entry);
        } catch (Exception e) {
            log.error("Error while logging change", e);
        }
    }

    private LogEntry createLogEntry(Object oldEntity, Object newEntity, OperationType operation) {
        String entityName = getEntityName(oldEntity, newEntity);
        String entityId = getEntityId(oldEntity, newEntity);

        return LogEntry.builder()
                .entityName(entityName)
                .entityId(entityId)
                .operation(operation.name())
                .build();
    }

    private String getEntityName(Object oldEntity, Object newEntity) {
        return (newEntity != null) ? newEntity.getClass().getSimpleName()
                : oldEntity.getClass().getSimpleName();
    }

    private String getEntityId(Object oldEntity, Object newEntity) {
        Object entity = newEntity != null ? newEntity : oldEntity;
        Class<?> clazz = entity.getClass();
        Optional<Field> idField = findIdField(clazz);

        if (idField.isPresent()) {
            try {
                Field field = idField.get();
                field.setAccessible(true);
                if (!field.canAccess(entity)) {
                    return "Access Error";
                }
                Object idValue = field.get(entity);
                return idValue != null ? idValue.toString() : "null";
            } catch (IllegalAccessException e) {
                log.error("Error while accessing entity id", e);
                return "Access Error";
            }
        }
        return "Unknown";
    }

    private Optional<Field> findIdField(Class<?> clazz) {
        Optional<Field> idField = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(jakarta.persistence.Id.class))
                .findFirst();

        if (!idField.isPresent() && clazz.getSuperclass() != null) {
            return findIdField(clazz.getSuperclass());
        }
        return idField;
    }

    @LoggingMetrics({ LoggingMetrics.MetricType.QUEUE_SIZE, LoggingMetrics.MetricType.PROCESSED_COUNT,
            LoggingMetrics.MetricType.ERROR_COUNT })
    private void offerToQueue(LogEntry entry) {
        try {
            if (logQueue.remainingCapacity() == 0) {
                log.error("Log queue is full, skipping log entry");
                return;
            }
            boolean offered = logQueue.offer(entry, 100, TimeUnit.MILLISECONDS);
            if (!offered) {
                log.error("Failed to offer log entry to queue");
            }
        } catch (Exception e) {
            log.error("Error while offering to queue", e);
        }
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
            } catch (Exception e) {
                log.error("Error while processing logs", e);
            }
        }
    }

    private LogEntry pollFromQueue() throws InterruptedException {
        return logQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    @LoggingMetrics({ LoggingMetrics.MetricType.QUEUE_SIZE, LoggingMetrics.MetricType.BATCH_QUEUE_SIZE,
            LoggingMetrics.MetricType.PROCESSED_COUNT, LoggingMetrics.MetricType.ERROR_COUNT })
    private void processLogEntry(LogEntry entry) {
        if(entry == null) return;
        batchQueue.add(entry);
        if (batchQueue.size() >= batchSize) {
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
            if (toSave.size() >= batchSize) {
                break;
            }
        }
        return toSave;
    }

    @LoggingMetrics({ LoggingMetrics.MetricType.BATCH_QUEUE_SIZE, LoggingMetrics.MetricType.ERROR_COUNT })
    private void saveBatch(List<LogEntry> toSave) {
        try {
            logEntryRepository.saveAll(toSave);
        } catch (Exception e) {
            log.error("Error while saving batch", e);
        }
    }

    @Scheduled(fixedDelay = 5000)
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