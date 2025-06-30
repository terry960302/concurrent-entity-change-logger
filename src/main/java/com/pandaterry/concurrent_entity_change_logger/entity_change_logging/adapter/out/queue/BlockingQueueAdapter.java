package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.queue;


import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.mapper.LogEntryMapper;
import com.pandaterry.concurrent_entity_change_logger.shared.config.EntityLoggingProperties;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.entity.LogEntryJpo;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.QueuePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockingQueueAdapter implements QueuePort {

    private final EntityLoggingProperties loggingProperties;
    private final LogEntryMapper logEntryMapper;

    private BlockingQueue<LogEntryJpo> logQueue;
    private volatile boolean shutdown = false;

    @Override
    public void init() {
        this.logQueue = new LinkedBlockingQueue<>(
                loggingProperties.getStrategy().getQueueSize()
        );
    }

    @Override
    public void shutdown() {
        if (shutdown) {
            log.debug("이미 shutdown 상태입니다");
            return;
        }

        log.info("BlockingQueue shutdown 시작 - 남은 데이터: {}개", size());
        shutdown = true;

        try {
            awaitBriefCompletion();
            handleRemainingData();
            clearQueue();

            log.info("BlockingQueue shutdown 완료");

        } catch (Exception e) {
            log.error("Shutdown 중 오류 발생", e);
            clearQueue();
        }
    }

    @Override
    public boolean offer(LogEntry entry) {
        if (shutdown) {
            log.debug("Shutdown 상태 - offer 거부");
            return false;
        }

        try {
            LogEntryJpo jpo = logEntryMapper.toEntity(entry);
            return logQueue.offer(jpo);
        } catch (Exception e) {
            log.error("Offer 실패", e);
            return false;
        }
    }

    @Override
    public int drainTo(List<LogEntry> batch, int maxElements) {
        List<LogEntryJpo> jpoBatch = new java.util.ArrayList<>(maxElements);
        int drained = logQueue.drainTo(jpoBatch, maxElements);

        List<LogEntry> domainBatch = jpoBatch.stream()
                .map(logEntryMapper::toDomain).toList();
        batch.addAll(domainBatch);

        return drained;
    }

    @Override
    public int remainingCapacity() {
        return logQueue.remainingCapacity();
    }

    @Override
    public int size() {
        return logQueue.size();
    }

    private void awaitBriefCompletion() {
        int waitSeconds = 5;
        log.debug("{}초간 완료 대기 시작", waitSeconds);

        for (int i = 0; i < waitSeconds; i++) {
            if (logQueue.isEmpty()) {
                log.debug("큐가 비어있음 - 대기 종료");
                return;
            }

            try {
                Thread.sleep(1000);
                log.debug("완료 대기 중... 남은 데이터: {}개", size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("대기 중 인터럽트 발생");
                return;
            }
        }

        log.debug("완료 대기 타임아웃 - 강제 처리로 진행");
    }

    private void handleRemainingData() {
        int remainingCount = size();
        if (remainingCount == 0) {
            log.debug("처리할 남은 데이터가 없습니다");
            return;
        }

        logRemainingData();
    }

    private void logRemainingData() {
        List<LogEntryJpo> remaining = new ArrayList<>();
        logQueue.drainTo(remaining);

        log.warn("드롭되는 로그 엔트리 {}개:", remaining.size());

        // 처음 10개만 상세 로그
        remaining.stream()
                .limit(10)
                .forEach(jpo -> log.warn("드롭 엔트리 - Entity: {}, Operation: {}",
                        jpo.getEntityName(), jpo.getOperation()));

        if (remaining.size() > 10) {
            log.warn("... 외 {}개 더 드롭됨", remaining.size() - 10);
        }
    }

    private void clearQueue() {
        if (logQueue != null) {
            logQueue.clear();
            logQueue = null;
        }
    }
}
