package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.lifecycle;


import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.LogStoragePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.MetricsPort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.QueuePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.service.BatchProcessingService;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.error.LoggingErrorCode;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.error.LoggingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingSmartLifecycle implements SmartLifecycle {
    private final LogStoragePort logStoragePort;
    private final QueuePort queuePort;
    private final MetricsPort metricsPort;
    private final BatchProcessingService batchProcessingService;

    private volatile boolean running = false;

    @Override
    public void start() {
        if (running) {
            return;
        }

        log.info("로깅 인프라 시작");
        try {
            logStoragePort.init();
            queuePort.init();
            batchProcessingService.init();

            running = true;
            log.info("로깅 인프라 시작 완료");
        } catch (Exception e) {
            log.error("로깅 인프라 시작 실패", e);
            throw LoggingException.of(LoggingErrorCode.FAILED_INIT_LOGGING_INFRA, e);
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        log.info("로깅 인프라 정상 종료 시작");
        gracefulShutdown();
        running = false;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private void gracefulShutdown() {
        try {
            batchProcessingService.shutdown();
            queuePort.shutdown();

            // 진행 중인 작업 완료 대기
            awaitTermination(Duration.ofSeconds(30));

            // 최종 플러시 처리
            batchProcessingService.finalFlush();

            // 리소스 정리
            logStoragePort.close();
            // 모니터링 종료
            metricsPort.shutdown();

        } catch (Exception e) {
            log.error("종료 중 오류 발생", e);
        }
    }

    private boolean awaitTermination(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (queuePort.size() > 0 && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return queuePort.size() == 0;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1000; // 다른 컴포넌트보다 늦게 종료
    }
}
