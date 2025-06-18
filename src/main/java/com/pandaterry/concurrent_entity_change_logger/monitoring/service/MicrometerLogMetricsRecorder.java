package com.pandaterry.concurrent_entity_change_logger.monitoring.service;

import com.pandaterry.concurrent_entity_change_logger.monitoring.constants.MetricNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MicrometerLogMetricsRecorder {
    private final Counter droppedCounter;
    private final Counter saveErrorCounter;
    private final Counter processedCounter;
    private final DistributionSummary batchSizeSummary;
    private final Gauge queueSizeGauge;
    private final Timer offerTimer;
    private final Timer batchTimer;
    private final Timer flushTimer;
    private final Counter shutdownFlushedCounter;
    private final AtomicInteger queueSize;

    public MicrometerLogMetricsRecorder(MeterRegistry registry) {
        this.droppedCounter = registry.counter(MetricNames.ENTITYLOG_QUEUE_DROPPED);
        this.saveErrorCounter = registry.counter(MetricNames.ENTITYLOG_BATCH_SAVE_ERRORS);
        this.processedCounter = registry.counter(MetricNames.ENTITYLOG_BATCH_PROCESSED);
        this.batchSizeSummary = registry.summary(MetricNames.ENTITYLOG_BATCH_SIZE);
        this.queueSize = new AtomicInteger(0);
        this.queueSizeGauge = Gauge
                .builder(MetricNames.ENTITYLOG_QUEUE_SIZE, queueSize, AtomicInteger::get)
                .register(registry);
        this.offerTimer = registry.timer(MetricNames.ENTITYLOG_QUEUE_OFFER_LATENCY);
        this.batchTimer = registry.timer(MetricNames.ENTITYLOG_BATCH_PROCESSING_LATENCY);
        this.flushTimer = registry.timer(MetricNames.ENTITYLOG_FLUSH_LATENCY);
        this.shutdownFlushedCounter = registry.counter(MetricNames.ENTITYLOG_SHUTDOWN_FLUSHED_COUNT);
    }

    public void recordOfferLatency(long durationNanos) {
        offerTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void incrementDroppedCount() {
        droppedCounter.increment();
    }

    public void gaugeQueueSize(int currentSize) {
        queueSize.set(currentSize);
    }

    public void recordBatchLatency(long durationNanos) {
        batchTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordBatchSize(int size) {
        batchSizeSummary.record(size);
    }

    public void incrementProcessedCount(int count) {
        processedCounter.increment(count);
    }

    public void incrementSaveErrorCount() {
        saveErrorCounter.increment();
    }

    public void recordFlushLatency(long durationNanos) {
        flushTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void incrementShutdownFlushedCount(int count) {
        shutdownFlushedCounter.increment(count);
    }
}
