package com.pandaterry.concurrent_entity_change_logger.core.application.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pandaterry.concurrent_entity_change_logger.core.domain.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.domain.Operation;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.persistence.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.persistence.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.config.EntityLoggingProperties;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage.LogStorage;
import com.pandaterry.concurrent_entity_change_logger.monitoring.service.MicrometerLogMetricsRecorder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class BlockingQueueLoggingStrategy implements LoggingStrategy {
    private final LogEntryRepository logEntryRepository;
    private final EntityLoggingProperties loggingProperties;
    private final LogEntryFactory logEntryFactory;
    private final ObjectMapper objectMapper;

    private final LogStorage logStorage;
    private final MicrometerLogMetricsRecorder metrics;

    private BlockingQueue<LogEntry> logQueue;
    private ExecutorService logProcessorPool;

    public BlockingQueueLoggingStrategy(LogEntryRepository logEntryRepository,
                                        EntityLoggingProperties loggingProperties,
                                        LogEntryFactory logEntryFactory,
                                        LogStorage logStorage,
                                        ObjectMapper objectMapper,
                                        MicrometerLogMetricsRecorder metrics) {
        this.logEntryRepository = logEntryRepository;
        this.loggingProperties = loggingProperties;
        this.logEntryFactory = logEntryFactory;
        this.metrics = metrics;
        this.logStorage = logStorage;
        this.objectMapper = objectMapper;
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
    public void logChange(Object oldEntity, Object newEntity, Operation operation) {
        if (!loggingProperties.shouldLogChanges(oldEntity != null ? oldEntity : newEntity)) {
            return;
        }
        LogEntry entry = logEntryFactory.create(oldEntity, newEntity, operation);
        try {
            logStorage.write(entry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        offerToQueue(entry);
    }

    private void offerToQueue(LogEntry entry) {
        long startNanos = System.nanoTime();
        long tookNanos = System.nanoTime() - startNanos;

        if (logQueue.remainingCapacity() == 0)
            return;
        boolean offered = logQueue.offer(entry);

        metrics.recordOfferLatency(tookNanos);
        if (!offered) {
            metrics.incrementDroppedCount();
        }
        metrics.gaugeQueueSize(logQueue.size());
    }

    /**
     * 배치를 저장시 batchUpdate에서 안하는 이유
     * : 스케쥴러로 동시에 배치업로드하는 방식이라 그럼. 배치를 repository 레이어에서 관리하는 순간 스케쥴러에서 관리가 어려워짐.
     */
    private void processLogs() {
        int batchSize = loggingProperties.getJpaBatchSize();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                while (logQueue.size() < batchSize) { // 배치사이즈에 도달하지 않으면 flush 스케쥴러에서 drainsTo 하기전에 미리 처리해줌.
                    Thread.sleep(50);
                }

                List<LogEntry> batch = new ArrayList<>(batchSize);

                // 동시성을 고려하여 batch.add 가 아닌 큐에서 바로 drainsTo 를 사용함.
                int drained = logQueue.drainTo(batch, batchSize);
                if (drained > 0) {
                    saveBatch(batch);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void saveBatch(List<LogEntry> toSave) {
        int success = 0;
        int batchCount = loggingProperties.getJpaBatchSize();
        long startNanos = System.nanoTime();
        try {
            logEntryRepository.saveBatch(toSave);
            success++;
        } catch (Exception e) {
            metrics.incrementSaveErrorCount();
        }

        long tookNanos = System.nanoTime() - startNanos;
        metrics.recordBatchSize(batchCount);
        metrics.recordBatchLatency(tookNanos);
        metrics.incrementProcessedCount(success);
        metrics.gaugeQueueSize(logQueue.size());
    }

    // 배치사이즈까지 안모으고 바로 플러시
    @Scheduled(fixedDelayString = "${logging.strategy.flush-interval:5000}")
    @Override
    public int flush() {
        int batchSize = loggingProperties.getJpaBatchSize();
        List<LogEntry> batch = new ArrayList<>(batchSize);
        int drained = logQueue.drainTo(batch, batchSize);
        if (drained > 0) {
            // 플러시 지연 측정
            long start = System.nanoTime();
            saveBatch(batch);
            long took = System.nanoTime() - start;
            metrics.recordFlushLatency(took);
        }
        // (선택) 큐 사이즈 계측
        metrics.gaugeQueueSize(logQueue.size());
        return drained;
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
        int flushed = flush();
        logStorage.close();
        metrics.incrementShutdownFlushedCount(flushed);
    }
}