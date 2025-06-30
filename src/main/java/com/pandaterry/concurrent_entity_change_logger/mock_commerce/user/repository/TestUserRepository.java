package com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.repository;

import com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.entity.TestUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestUserRepository extends JpaRepository<TestUser, Long> {
}