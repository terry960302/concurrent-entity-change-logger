package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.in.monitoring;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.MetricsPort;
import com.pandaterry.concurrent_entity_change_logger.shared.constant.Metrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MicrometerLogMetricsAdapter implements MetricsPort {
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

    public MicrometerLogMetricsAdapter(MeterRegistry registry) {
        this.droppedCounter = registry.counter(Metrics.ENTITYLOG_QUEUE_DROPPED);
        this.saveErrorCounter = registry.counter(Metrics.ENTITYLOG_BATCH_SAVE_ERRORS);
        this.processedCounter = registry.counter(Metrics.ENTITYLOG_BATCH_PROCESSED);
        this.batchSizeSummary = registry.summary(Metrics.ENTITYLOG_BATCH_SIZE);
        this.queueSize = new AtomicInteger(0);
        this.queueSizeGauge = Gauge
                .builder(Metrics.ENTITYLOG_QUEUE_SIZE, queueSize, AtomicInteger::get)
                .register(registry);
        this.offerTimer = registry.timer(Metrics.ENTITYLOG_QUEUE_OFFER_LATENCY);
        this.batchTimer = registry.timer(Metrics.ENTITYLOG_BATCH_PROCESSING_LATENCY);
        this.flushTimer = registry.timer(Metrics.ENTITYLOG_FLUSH_LATENCY);
        this.shutdownFlushedCounter = registry.counter(Metrics.ENTITYLOG_SHUTDOWN_FLUSHED_COUNT);
    }

    @Override
    public void recordOfferLatency(long durationNanos) {
        offerTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void incrementDroppedCount() {
        droppedCounter.increment();
    }

    @Override
    public void gaugeQueueSize(int currentSize) {
        queueSize.set(currentSize);
    }

    @Override
    public void recordBatchLatency(long durationNanos) {
        batchTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordBatchSize(int size) {
        batchSizeSummary.record(size);
    }

    @Override
    public void incrementProcessedCount(int count) {
        processedCounter.increment(count);
    }

    @Override
    public void incrementSaveErrorCount() {
        saveErrorCounter.increment();
    }

    @Override
    public void recordFlushLatency(long durationNanos) {
        flushTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void incrementShutdownFlushedCount(int count) {
        shutdownFlushedCounter.increment(count);
    }

    @Override
    public void shutdown() {
        droppedCounter.close();
        saveErrorCounter.close();
        processedCounter.close();
        batchSizeSummary.close();
        queueSizeGauge.close();
        offerTimer.close();
        flushTimer.close();
        shutdownFlushedCounter.close();
    }
}
