package com.pandaterry.concurrent_entity_change_logger.core.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.factory.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.repository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.tracker.EntityChangeTracker;
import com.pandaterry.concurrent_entity_change_logger.core.util.EntityLoggingCondition;
import com.pandaterry.concurrent_entity_change_logger.monitoring.annotation.LoggingMetrics;

import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;
    private final EntityChangeTracker changeTracker;
    private final EntityLoggingCondition loggingCondition;
    private final LogEntryFactory logEntryFactory;

    public BlockingQueueLoggingStrategy(LogEntryRepository logEntryRepository, EntityManager entityManager,
            EntityChangeTracker changeTracker, EntityLoggingCondition loggingCondition,
            LogEntryFactory logEntryFactory) {
        this.logEntryRepository = logEntryRepository;
        this.entityManager = entityManager;
        this.changeTracker = changeTracker;
        this.loggingCondition = loggingCondition;
        this.logEntryFactory = logEntryFactory;
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
        if (!loggingCondition.shouldLogChanges(oldEntity != null ? oldEntity : newEntity)) {
            return;
        }
        LogEntry entry = logEntryFactory.create(oldEntity, newEntity, operation);
        offerToQueue(entry);
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
            Field field = idField.get();
            field.setAccessible(true);
            if (!field.canAccess(entity)) {
                return "Access Error";
            }
            Object idValue = getFieldValue(field, entity);
            return idValue != null ? idValue.toString() : "null";
        }
        return "Unknown";
    }

    private Object getFieldValue(Field field, Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
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