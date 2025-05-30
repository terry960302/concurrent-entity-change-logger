package com.pandaterry.concurrent_entity_change_logger.monitoring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggingMetrics {
    MetricType[] value() default {};

    enum MetricType {
        PROCESSING_TIME,
        QUEUE_SIZE,
        BATCH_QUEUE_SIZE,
        PROCESSED_COUNT,
        ERROR_COUNT
    }
}