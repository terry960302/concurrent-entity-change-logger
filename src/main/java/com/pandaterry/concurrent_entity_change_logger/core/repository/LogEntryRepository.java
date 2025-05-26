package com.pandaterry.concurrent_entity_change_logger.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
}
