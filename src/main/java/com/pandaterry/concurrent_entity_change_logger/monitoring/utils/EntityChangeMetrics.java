package com.pandaterry.concurrent_entity_change_logger.monitoring.utils;

import org.springframework.stereotype.Component;

import com.pandaterry.concurrent_entity_change_logger.monitoring.constants.MetricNames;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class EntityChangeMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter processedLogCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    
    public EntityChangeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.processedLogCounter = Counter.builder(MetricNames.LOG_PROCESSOR_LOGS_TOTAL)
            .description("Total number of processed logs")
            .register(meterRegistry);
            
        this.errorCounter = Counter.builder(MetricNames.LOG_PROCESSOR_ERRORS_TOTAL)
            .description("Total number of processing errors")
            .register(meterRegistry);
            
        this.processingTimer = Timer.builder(MetricNames.LOG_PROCESSOR_PROCESSING_TIME)
            .description("Time taken to process logs")
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
}
