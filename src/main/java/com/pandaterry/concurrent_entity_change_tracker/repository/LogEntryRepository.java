package com.pandaterry.concurrent_entity_change_tracker.repository;

import com.pandaterry.concurrent_entity_change_tracker.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
}
