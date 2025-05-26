package com.pandaterry.concurrent_entity_change_logger.loadtest.repository;

import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.TestUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestUserRepository extends JpaRepository<TestUser, Long> {
}