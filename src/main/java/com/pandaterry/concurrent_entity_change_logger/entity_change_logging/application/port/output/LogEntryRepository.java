package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.port.output;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;

import java.util.List;

public interface LogEntryRepository {
    List<LogEntry> saveAll(List<LogEntry> toSave);

    List<LogEntry> findAll();

    void saveBatch(List<LogEntry> toSave);

    void saveBatchWithChunking(List<LogEntry> toSave, int chunkSize);
}