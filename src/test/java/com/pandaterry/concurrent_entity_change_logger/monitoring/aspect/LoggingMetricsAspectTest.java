package com.pandaterry.concurrent_entity_change_logger.monitoring.aspect;

import com.pandaterry.concurrent_entity_change_logger.core.strategy.BlockingQueueLoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.monitoring.annotation.LoggingMetrics;
import com.pandaterry.concurrent_entity_change_logger.monitoring.service.EntityChangeMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingMetricsAspectTest {

    @Mock
    private EntityChangeMetrics metrics;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private BlockingQueueLoggingStrategy target;

    private LoggingMetricsAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new LoggingMetricsAspect(metrics);
        when(joinPoint.getTarget()).thenReturn(target);
    }

    @Test
    void measureMetrics_ShouldRecordProcessingTime() throws Throwable {
        // given
        LoggingMetrics loggingMetrics = createLoggingMetrics(LoggingMetrics.MetricType.PROCESSING_TIME);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        aspect.measureMetrics(joinPoint, loggingMetrics);

        // then
        verify(metrics).startProcessingTimer();
        verify(metrics).stopProcessingTimer(any());
    }

    @Test
    void measureMetrics_ShouldRecordError() throws Throwable {
        // given
        LoggingMetrics loggingMetrics = createLoggingMetrics(LoggingMetrics.MetricType.ERROR_COUNT);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test error"));

        // when
        try {
            aspect.measureMetrics(joinPoint, loggingMetrics);
        } catch (RuntimeException e) {
            // expected
        }

        // then
        verify(metrics).recordError();
    }

    @Test
    void measureMetrics_ShouldRecordQueueSize() throws Throwable {
        // given
        LoggingMetrics loggingMetrics = createLoggingMetrics(LoggingMetrics.MetricType.QUEUE_SIZE);
        BlockingQueue<?> queue = new LinkedBlockingQueue<>();
        ReflectionTestUtils.setField(target, "logQueue", queue);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        aspect.measureMetrics(joinPoint, loggingMetrics);

        // then
        verify(metrics).updateQueueSize(queue);
    }

    @Test
    void measureMetrics_ShouldRecordBatchQueueSize() throws Throwable {
        // given
        LoggingMetrics loggingMetrics = createLoggingMetrics(LoggingMetrics.MetricType.BATCH_QUEUE_SIZE);
        ConcurrentLinkedQueue<?> batchQueue = new ConcurrentLinkedQueue<>();
        ReflectionTestUtils.setField(target, "batchQueue", batchQueue);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        aspect.measureMetrics(joinPoint, loggingMetrics);

        // then
        verify(metrics).updateBatchQueueSize(batchQueue);
    }

    @Test
    void measureMetrics_ShouldRecordProcessedCount() throws Throwable {
        // given
        LoggingMetrics loggingMetrics = createLoggingMetrics(LoggingMetrics.MetricType.PROCESSED_COUNT);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        aspect.measureMetrics(joinPoint, loggingMetrics);

        // then
        verify(metrics).recordProcessedLog();
    }

    @Test
    void measureMetrics_ShouldHandleMultipleMetrics() throws Throwable {
        // given
        LoggingMetrics loggingMetrics = createLoggingMetrics(
                LoggingMetrics.MetricType.PROCESSING_TIME,
                LoggingMetrics.MetricType.ERROR_COUNT,
                LoggingMetrics.MetricType.PROCESSED_COUNT);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        aspect.measureMetrics(joinPoint, loggingMetrics);

        // then
        verify(metrics).startProcessingTimer();
        verify(metrics).stopProcessingTimer(any());
        verify(metrics).recordProcessedLog();
    }

    private LoggingMetrics createLoggingMetrics(LoggingMetrics.MetricType... types) {
        return new LoggingMetrics() {
            @Override
            public MetricType[] value() {
                return types;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return LoggingMetrics.class;
            }
        };
    }
}