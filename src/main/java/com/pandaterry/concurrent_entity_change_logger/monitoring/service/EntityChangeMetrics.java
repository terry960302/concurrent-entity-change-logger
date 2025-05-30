package com.pandaterry.concurrent_entity_change_logger.monitoring.service;

import com.pandaterry.concurrent_entity_change_logger.monitoring.constants.MetricNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class EntityChangeMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter processedLogCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    private final AtomicInteger queueSize;
    private final AtomicInteger batchQueueSize;

    public EntityChangeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.queueSize = new AtomicInteger(0);
        this.batchQueueSize = new AtomicInteger(0);

        // Counter 메트릭 등록
        this.processedLogCounter = Counter.builder(MetricNames.LOG_PROCESSOR_LOGS_TOTAL)
                .description("Total number of processed logs")
                .register(meterRegistry);

        this.errorCounter = Counter.builder(MetricNames.LOG_PROCESSOR_ERRORS_TOTAL)
                .description("Total number of processing errors")
                .register(meterRegistry);

        // Timer 메트릭 등록
        this.processingTimer = Timer.builder(MetricNames.LOG_PROCESSOR_PROCESSING_TIME)
                .description("Time taken to process logs")
                .register(meterRegistry);

        // Gauge 메트릭 등록
        Gauge.builder(MetricNames.LOG_PROCESSOR_QUEUE_SIZE, queueSize, AtomicInteger::get)
                .description("Current size of the log queue")
                .register(meterRegistry);

        Gauge.builder(MetricNames.LOG_PROCESSOR_BATCH_QUEUE_SIZE, batchQueueSize, AtomicInteger::get)
                .description("Current size of the batch queue")
                .register(meterRegistry);
    }

    public void recordProcessedLog() {
        processedLogCounter.increment();
    }

    public void recordError() {
        errorCounter.increment();
    }

    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopProcessingTimer(Timer.Sample sample) {
        sample.stop(processingTimer);
    }

    public void updateQueueSize(BlockingQueue<?> queue) {
        queueSize.set(queue.size());
    }

    public void updateBatchQueueSize(ConcurrentLinkedQueue<?> queue) {
        batchQueueSize.set(queue.size());
    }
}
