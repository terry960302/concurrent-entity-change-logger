package com.pandaterry.concurrent_entity_change_logger.loadtest.repository;

import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.TestOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestOrderRepository extends JpaRepository<TestOrder, Long> {
}