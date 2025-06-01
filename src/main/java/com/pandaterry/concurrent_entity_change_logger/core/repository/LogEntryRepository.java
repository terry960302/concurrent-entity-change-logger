package com.pandaterry.concurrent_entity_change_logger.core.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
       @Modifying
       @Query(value = "INSERT INTO log_entries (entity_name, entity_id, operation, changes) VALUES (:entityName, :entityId, :operation, :changes)", nativeQuery = true)
       void batchInsert(@Param("entityName") String entityName,
                     @Param("entityId") String entityId,
                     @Param("operation") String operation,
                     @Param("changes") String changes);
}
