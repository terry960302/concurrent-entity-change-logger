package com.pandaterry.concurrent_entity_change_logger.loadtest.repository;

import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.TestProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestProductRepository extends JpaRepository<TestProduct, Long> {
}