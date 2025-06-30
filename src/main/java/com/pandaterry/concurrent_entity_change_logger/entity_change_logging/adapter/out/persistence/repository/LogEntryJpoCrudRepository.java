package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.repository;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.entity.LogEntryJpo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LogEntryJpoCrudRepository extends JpaRepository<LogEntryJpo, UUID> {
}
