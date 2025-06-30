package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.mapper.LogEntryMapper;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.shared.constant.LogEntrySql;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.entity.LogEntryJpo;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LogEntryRepositoryImpl implements LogEntryRepository {

    private final LogEntryJpoCrudRepository crudRepository;

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final LogEntryMapper logEntryMapper;

    @Transactional
    public List<LogEntry> saveAll(List<LogEntry> logEntries) {
        List<LogEntryJpo> saved = crudRepository.saveAll(logEntries.stream().map(logEntryMapper::toEntity).toList());
        return saved.stream().map(logEntryMapper::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<LogEntry> findAll() {
        return crudRepository.findAll().stream().map(logEntryMapper::toDomain).toList();
    }


    @Transactional
    public void saveBatch(List<LogEntry> toSave) {
        if (toSave == null || toSave.isEmpty()) {
            log.warn("저장할 로그 엔트리가 없습니다.");
            return;
        }

        List<Object[]> batchArgs = toSave.stream()
                .map(logEntryMapper::toEntity)
                .map(this::convertToObjectArray)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!batchArgs.isEmpty()) {
            try {
                int[] results = jdbcTemplate.batchUpdate(LogEntrySql.INSERT, batchArgs);
                log.info("배치 저장 완료: {} 건", results.length);

                // 실패한 건수 체크
                long failedCount = java.util.Arrays.stream(results)
                        .filter(result -> result == PreparedStatement.EXECUTE_FAILED)
                        .count();

                if (failedCount > 0) {
                    log.warn("배치 처리 중 실패한 건수: {}", failedCount);
                }

            } catch (Exception e) {
                log.error("배치 저장 실패", e);
                throw new RuntimeException("로그 엔트리 배치 저장 실패", e);
            }
        }
    }

    @Transactional
    public void saveBatchWithChunking(List<LogEntry> toSave, int chunkSize) {
        if (toSave == null || toSave.isEmpty()) {
            return;
        }

        int totalSize = toSave.size();
        int processedCount = 0;

        for (int i = 0; i < totalSize; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, totalSize);
            List<LogEntry> chunk = toSave.subList(i, endIndex);

            saveBatch(chunk);
            processedCount += chunk.size();

            log.debug("청크 처리 진행률: {}/{}", processedCount, totalSize);
        }

        log.info("전체 배치 처리 완료: {} 건", processedCount);
    }

    private Object[] convertToObjectArray(LogEntryJpo entry) {
        try {
            String changesJson = objectMapper.writeValueAsString(entry.getChanges());

            String contextJson = objectMapper.writeValueAsString(entry.getContext());

            return new Object[]{
                    entry.getId(),
                    entry.getEntityName(),
                    entry.getEntityId(),
                    changesJson,
                    entry.getOperation().name(),
                    Timestamp.from(entry.getRecordedAt()),
                    contextJson
            };
        } catch (Exception e) {
            log.error("LogEntry 변환 실패: {}", entry, e);
            return null;
        }
    }
}
