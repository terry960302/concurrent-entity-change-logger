package com.pandaterry.concurrent_entity_change_logger.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.repository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.monitoring.constants.MetricNames;
import com.pandaterry.concurrent_entity_change_logger.monitoring.service.EntityChangeMetrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer.Sample;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class EntityChangeLogger {

    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(100000);
    private final ExecutorService logProcessorPool;
    private final Queue<LogEntry> batchQueue = new ConcurrentLinkedQueue<>();
    private final ObjectMapper objectMapper;
    private final int batchSize = 1000;
    private final MeterRegistry meterRegistry;
    private final EntityChangeMetrics metrics;
    private final LogEntryRepository logEntryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EntityChangeLogger(MeterRegistry meterRegistry,
                              EntityChangeMetrics metrics,
                              LogEntryRepository logEntryRepository) {
        this.meterRegistry = meterRegistry;
        this.metrics = metrics;
        this.logEntryRepository = logEntryRepository;

        // 큐 게이지 등록
        Gauge.builder(MetricNames.LOG_PROCESSOR_QUEUE_SIZE, logQueue, BlockingQueue::size)
                .description("Current size of the log queue")
                .register(meterRegistry);

        Gauge.builder(MetricNames.LOG_PROCESSOR_BATCH_QUEUE_SIZE, batchQueue, Queue::size)
                .description("Current size of the batch queue")
                .register(meterRegistry);

        this.logProcessorPool = Executors.newFixedThreadPool(5);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        for (int i = 0; i < 5; i++) {
            logProcessorPool.submit(this::processLogs);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void logChange(Object oldEntity, Object newEntity, String operation) {
        Sample sample = metrics.startProcessingTimer();
        try {
            String entityName = (newEntity != null) ? newEntity.getClass().getSimpleName()
                    : oldEntity.getClass().getSimpleName();
            String entityId = getEntityId(newEntity != null ? newEntity : oldEntity);

            LogEntry entry = LogEntry.builder()
                    .entityName(entityName)
                    .entityId(entityId)
                    .operation(operation)
                    .build();
            if (!logQueue.offer(entry, 100, TimeUnit.MILLISECONDS)) {
                // 큐가 가득 찼을 경우 처리 로직 추가
                System.err.println("Log queue is full, skipping log entry");
            }
            metrics.recordProcessedLog();
        } catch (Exception e) {
            e.printStackTrace();
            metrics.recordError();
        } finally {
            metrics.stopProcessingTimer(sample);
        }
    }

    private void processLogs() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    batchQueue.add(entry);
                    if (batchQueue.size() >= batchSize) {
                        flushBatch();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Transactional
    private void flushBatch() {
        List<LogEntry> toSave = new ArrayList<>();
        LogEntry entry;
        while ((entry = batchQueue.poll()) != null) {
            toSave.add(entry);
            if (toSave.size() >= batchSize) {
                break;
            }
        }
        if (!toSave.isEmpty()) {
            logEntryRepository.saveAll(toSave);
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void scheduledFlush() {
        flushBatch();
    }

    public void shutdown() {
        logProcessorPool.shutdown();
        try {
            if (!logProcessorPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logProcessorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            logProcessorPool.shutdownNow();
        }
        scheduledFlush(); // 잔여 데이터 플러시
    }

    private String getEntityId(Object entity) {
        Class<?> clazz = entity.getClass();
        Optional<Field> idField = findIdField(clazz);

        if (idField.isPresent()) {
            try {
                Field field = idField.get();
                field.setAccessible(true);
                Object idValue = field.get(entity);
                return idValue != null ? idValue.toString() : "null";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return "Access Error";
            }
        }

        return "Unknown";
    }

    private Optional<Field> findIdField(Class<?> clazz) {
        // 현재 클래스에서 @Id 어노테이션이 달린 필드 찾기
        Optional<Field> idField = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst();

        // 현재 클래스에서 찾지 못했다면 부모 클래스에서 재귀적으로 찾기
        if (!idField.isPresent() && clazz.getSuperclass() != null) {
            return findIdField(clazz.getSuperclass());
        }

        return idField;
    }

}
