package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.batch;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.BatchPersistencePort;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchPersistenceAdapter implements BatchPersistencePort {

    private final LogEntryRepository logEntryRepository;

    @Override
    public void saveBatch(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            log.debug("저장할 로그 엔트리가 없습니다");
            return;
        }

        long startTime = System.currentTimeMillis();

        try {

            logEntryRepository.saveBatch(logEntries);

            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("배치 저장 완료 - 건수: {}, 처리시간: {}ms",
                    logEntries.size(), processingTime);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("배치 저장 실패 - 건수: {}, 처리시간: {}ms",
                    logEntries.size(), processingTime, e);
            throw e; // 상위로 예외 전파
        }
    }
}
