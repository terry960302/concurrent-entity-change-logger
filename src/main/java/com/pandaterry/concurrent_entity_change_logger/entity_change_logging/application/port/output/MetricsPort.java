package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output;

public interface MetricsPort {
    void recordOfferLatency(long durationNanos);

    void incrementDroppedCount();

    void gaugeQueueSize(int currentSize);

    void recordBatchLatency(long durationNanos);

    void recordBatchSize(int size);

    void incrementProcessedCount(int count);

    void incrementSaveErrorCount();

    void recordFlushLatency(long durationNanos);

    void incrementShutdownFlushedCount(int count);

    void shutdown();
}
