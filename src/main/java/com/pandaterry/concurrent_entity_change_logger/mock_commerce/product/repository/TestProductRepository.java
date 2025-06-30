package com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.repository;

import com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.entity.TestProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestProductRepository extends JpaRepository<TestProduct, Long> {
}