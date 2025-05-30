package com.pandaterry.concurrent_entity_change_logger.monitoring.aspect;

import com.pandaterry.concurrent_entity_change_logger.core.strategy.BlockingQueueLoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.monitoring.annotation.LoggingMetrics;
import com.pandaterry.concurrent_entity_change_logger.monitoring.service.EntityChangeMetrics;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Aspect
@Component
@RequiredArgsConstructor
public class LoggingMetricsAspect {
    private final EntityChangeMetrics metrics;

    @Around("@annotation(loggingMetrics)")
    public Object measureMetrics(ProceedingJoinPoint joinPoint, LoggingMetrics loggingMetrics) throws Throwable {
        Sample sample = null;
        if (containsMetricType(loggingMetrics, LoggingMetrics.MetricType.PROCESSING_TIME)) {
            sample = metrics.startProcessingTimer();
        }

        try {
            Object result = joinPoint.proceed();
            recordMetrics(joinPoint, loggingMetrics);
            return result;
        } catch (Exception e) {
            if (containsMetricType(loggingMetrics, LoggingMetrics.MetricType.ERROR_COUNT)) {
                metrics.recordError();
            }
            throw e;
        } finally {
            if (sample != null) {
                metrics.stopProcessingTimer(sample);
            }
        }
    }

    private void recordMetrics(ProceedingJoinPoint joinPoint, LoggingMetrics loggingMetrics) {
        Object target = joinPoint.getTarget();

        for (LoggingMetrics.MetricType metricType : loggingMetrics.value()) {
            try {
                switch (metricType) {
                    case QUEUE_SIZE:
                        if (target instanceof BlockingQueueLoggingStrategy) {
                            BlockingQueue<?> queue = (BlockingQueue<?>) target.getClass()
                                    .getDeclaredField("logQueue").get(target);
                            metrics.updateQueueSize(queue);
                        }
                        break;
                    case BATCH_QUEUE_SIZE:
                        if (target instanceof BlockingQueueLoggingStrategy) {
                            ConcurrentLinkedQueue<?> batchQueue = (ConcurrentLinkedQueue<?>) target.getClass()
                                    .getDeclaredField("batchQueue").get(target);
                            metrics.updateBatchQueueSize(batchQueue);
                        }
                        break;
                    case PROCESSED_COUNT:
                        metrics.recordProcessedLog();
                        break;
                }
            } catch (Exception e) {
                // 메트릭 수집 실패는 로깅만 하고 예외는 전파하지 않음
                org.slf4j.LoggerFactory.getLogger(LoggingMetricsAspect.class)
                        .error("Failed to record metric: " + metricType, e);
            }
        }
    }

    private boolean containsMetricType(LoggingMetrics loggingMetrics, LoggingMetrics.MetricType type) {
        for (LoggingMetrics.MetricType metricType : loggingMetrics.value()) {
            if (metricType == type) {
                return true;
            }
        }
        return false;
    }
}