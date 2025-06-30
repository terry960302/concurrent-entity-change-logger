package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.service;


import com.pandaterry.concurrent_entity_change_logger.shared.config.EntityLoggingProperties;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.BatchPersistencePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.MetricsPort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.QueuePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessingService {

    private final QueuePort queuePort;
    private final BatchPersistencePort batchPersistencePort;
    private final MetricsPort metricsPort;
    private final EntityLoggingProperties loggingProperties;

    private ExecutorService logProcessorPool;

    public void init() {
        this.logProcessorPool = Executors.newFixedThreadPool(
                loggingProperties.getStrategy().getThreadPoolSize()
        );

        for (int i = 0; i < loggingProperties.getStrategy().getThreadPoolSize(); i++) {
            logProcessorPool.submit(this::processLogs);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void processLogs() {
        int batchSize = loggingProperties.getJpaBatchSize();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                while (queuePort.size() < batchSize) {
                    Thread.sleep(50);
                }

                List<LogEntry> batch = new ArrayList<>(batchSize);
                int drained = queuePort.drainTo(batch, batchSize);

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
            batchPersistencePort.saveBatch(toSave);
            success++;
        } catch (Exception e) {
            metricsPort.incrementSaveErrorCount();
        }

        long tookNanos = System.nanoTime() - startNanos;
        metricsPort.recordBatchSize(batchCount);
        metricsPort.recordBatchLatency(tookNanos);
        metricsPort.incrementProcessedCount(success);
        metricsPort.gaugeQueueSize(queuePort.size());
    }

    public int flush() {
        int batchSize = loggingProperties.getJpaBatchSize();
        List<LogEntry> batch = new ArrayList<>(batchSize);
        int drained = queuePort.drainTo(batch, batchSize);

        if (drained > 0) {
            long start = System.nanoTime();
            saveBatch(batch);
            long took = System.nanoTime() - start;
            metricsPort.recordFlushLatency(took);
        }

        metricsPort.gaugeQueueSize(queuePort.size());
        return drained;
    }

    public int finalFlush() {
        log.info("최종 플러시 시작");

        int totalFlushed = 0;
        int flushRound = 0;

        // 큐가 비워질 때까지 반복 플러시
        while (queuePort.size() > 0 && flushRound < 5) { // 최대 5회 시도
            int flushed = flush();
            totalFlushed += flushed;
            flushRound++;

            if (flushed == 0) {
                break; // 더 이상 플러시할 데이터가 없음
            }
        }

        log.info("최종 플러시 완료 - 총 {} 건", totalFlushed);
        return totalFlushed;
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

        int flushed = flush();
        metricsPort.incrementShutdownFlushedCount(flushed);
    }
}
