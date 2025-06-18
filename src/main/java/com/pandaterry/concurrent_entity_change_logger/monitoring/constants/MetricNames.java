package com.pandaterry.concurrent_entity_change_logger.monitoring.constants;

public class MetricNames {
    private MetricNames(){}
    public static final String ENTITYLOG_QUEUE_DROPPED          = "entitylog.queue.dropped";
    public static final String ENTITYLOG_BATCH_SAVE_ERRORS      = "entitylog.batch.save.errors";
    public static final String ENTITYLOG_BATCH_PROCESSED        = "entitylog.batch.processed";
    public static final String ENTITYLOG_BATCH_SIZE             = "entitylog.batch.size";
    public static final String ENTITYLOG_QUEUE_SIZE             = "entitylog.queue.size";
    public static final String ENTITYLOG_QUEUE_OFFER_LATENCY    = "entitylog.queue.offer.latency";
    public static final String ENTITYLOG_BATCH_PROCESSING_LATENCY = "entitylog.batch.processing.latency";
    public static final String ENTITYLOG_FLUSH_LATENCY          = "entitylog.flush.latency";
    public static final String ENTITYLOG_SHUTDOWN_FLUSHED_COUNT = "entitylog.shutdown.flushed.count";
}
