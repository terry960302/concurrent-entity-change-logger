package com.pandaterry.concurrent_entity_change_logger.integration;

import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.factory.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.repository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.strategy.BlockingQueueLoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.tracker.EntityChangeTracker;
import com.pandaterry.concurrent_entity_change_logger.core.util.EntityLoggingCondition;
import com.pandaterry.concurrent_entity_change_logger.monitoring.service.EntityChangeMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LoggingStrategyIntegrationTest {

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityChangeTracker changeTracker;

    @Autowired
    private EntityLoggingCondition loggingCondition;

    @Autowired
    private LogEntryFactory logEntryFactory;

    private BlockingQueueLoggingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new BlockingQueueLoggingStrategy(logEntryRepository, entityManager, changeTracker, loggingCondition, logEntryFactory);
    }

    @Test
    void shouldLogEntityChanges() throws InterruptedException {
        // given
        TestEntity entity = new TestEntity(1L, "test");
        TestEntity newEntity = new TestEntity(1L, "updated");

        // when
        strategy.logChange(entity, newEntity, OperationType.UPDATE);
        strategy.flush(); // 배치 처리 강제 실행
        TimeUnit.MILLISECONDS.sleep(100); // 비동기 처리 대기

        // then
        List<LogEntry> entries = logEntryRepository.findAll();
        assertThat(entries).hasSize(1);
        LogEntry entry = entries.get(0);
        assertThat(entry.getEntityName()).isEqualTo("TestEntity");
        assertThat(entry.getEntityId()).isEqualTo("1");
        assertThat(entry.getOperation()).isEqualTo(OperationType.UPDATE.name());
    }

    @Test
    void shouldHandleMultipleChanges() throws InterruptedException {
        // given
        TestEntity entity1 = new TestEntity(1L, "test1");
        TestEntity entity2 = new TestEntity(2L, "test2");

        // when
        strategy.logChange(null, entity1, OperationType.INSERT);
        strategy.logChange(entity1, entity2, OperationType.UPDATE);
        strategy.flush();
        TimeUnit.MILLISECONDS.sleep(100);

        // then
        List<LogEntry> entries = logEntryRepository.findAll();
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(LogEntry::getOperation)
                .containsExactlyInAnyOrder(OperationType.INSERT.name(), OperationType.UPDATE.name());
    }

    @Test
    void shouldHandleConcurrentChanges() throws InterruptedException {
        // given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                TestEntity entity = new TestEntity((long) index, "test" + index);
                strategy.logChange(null, entity, OperationType.INSERT);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        strategy.flush();
        TimeUnit.MILLISECONDS.sleep(100);

        // then
        List<LogEntry> entries = logEntryRepository.findAll();
        assertThat(entries).hasSize(threadCount);
        assertThat(entries).extracting(LogEntry::getOperation)
                .allMatch(op -> op.equals(OperationType.INSERT.name()));
    }

    private static class TestEntity {
        @Id
        private Long id;
        private String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public EntityChangeMetrics entityChangeMetrics(MeterRegistry meterRegistry) {
            return new EntityChangeMetrics(meterRegistry);
        }
    }
}