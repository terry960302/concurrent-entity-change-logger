package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pandaterry.concurrent_entity_change_logger.core.domain.LogEntry;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LogEntryRepository {
    private final EntityManager em;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<LogEntry> saveAll(List<LogEntry> logEntries){
        List<LogEntry> savedAll = new ArrayList<>();

        if (logEntries == null || logEntries.isEmpty()) {
            log.warn("저장할 로그 엔트리가 없습니다.");
            return savedAll;
        }
        String sqlTemplate = """
            INSERT INTO log_entries (id, entity_name, entity_id, operation, changes, created_at) 
            VALUES (%s, %s, %s, %s, %s::jsonb, %s)
            """;

        for(LogEntry entry : logEntries){
            String sql = String.format(sqlTemplate, entry.getEntityName(), entry.getId(), entry.getOperation(), entry.getChanges(), entry.getCreatedAt());
            LogEntry saved = jdbcTemplate.queryForObject(sql, LogEntry.class);
            savedAll.add(saved);
        }
        return savedAll;
    }

    @Transactional(readOnly = true)
    public List<LogEntry> findAll(){
        String sql = "SELECT * FROM log_entries";
        return jdbcTemplate.queryForList(sql, LogEntry.class);
    }


    @Transactional
    public void saveBatch(List<LogEntry> toSave) {
        if (toSave == null || toSave.isEmpty()) {
            log.warn("저장할 로그 엔트리가 없습니다.");
            return;
        }

        String sql = """
            INSERT INTO log_entries (id, entity_name, entity_id, operation, changes, created_at) 
            VALUES (?::uuid, ?, ?, ?, ?::jsonb, ?)
            """;

        List<Object[]> batchArgs = toSave.stream()
                .map(this::convertToObjectArray)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!batchArgs.isEmpty()) {
            try {
                int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
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

    private Object[] convertToObjectArray(LogEntry entry) {
        try {
            String changesJson = objectMapper.writeValueAsString(entry.getChanges());
            LocalDateTime createdAt = entry.getCreatedAt() != null ?
                    entry.getCreatedAt() : LocalDateTime.now();

            return new Object[]{
                    UUID.randomUUID(),
                    entry.getEntityName(),
                    entry.getEntityId(),
                    entry.getOperation().name(),
                    changesJson,
                    Timestamp.valueOf(createdAt),
            };
        } catch (Exception e) {
            log.error("LogEntry 변환 실패: {}", entry, e);
            return null;
        }
    }
}
