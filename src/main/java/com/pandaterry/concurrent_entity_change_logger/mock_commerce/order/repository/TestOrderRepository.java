package com.pandaterry.concurrent_entity_change_logger.mock_commerce.order.repository;

import com.pandaterry.concurrent_entity_change_logger.mock_commerce.order.entity.TestOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestOrderRepository extends JpaRepository<TestOrder, Long> {
}