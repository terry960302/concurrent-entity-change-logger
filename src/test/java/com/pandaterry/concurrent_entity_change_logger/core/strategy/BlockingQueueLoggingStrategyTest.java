package com.pandaterry.concurrent_entity_change_logger.core.strategy;

import com.pandaterry.concurrent_entity_change_logger.core.entity.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.factory.LogEntryFactory;
import com.pandaterry.concurrent_entity_change_logger.core.repository.LogEntryRepository;
import com.pandaterry.concurrent_entity_change_logger.core.tracker.EntityChangeTracker;
import com.pandaterry.concurrent_entity_change_logger.core.util.EntityLoggingCondition;
import jakarta.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockingQueueLoggingStrategyTest {

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private EntityChangeTracker changeTracker;

    @Mock
    private EntityLoggingCondition loggingCondition;

    @Mock
    private LogEntryFactory logEntryFactory;

    private BlockingQueueLoggingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new BlockingQueueLoggingStrategy(logEntryRepository, null, changeTracker, loggingCondition,
                logEntryFactory);
        when(loggingCondition.shouldLogChanges(any())).thenReturn(true);
    }

    @Test
    void logChange_ShouldCreateAndQueueLogEntry() {
        // given
        TestEntity entity = new TestEntity(1L, "test");
        TestEntity newEntity = new TestEntity(1L, "updated");
        LogEntry expectedEntry = LogEntry.builder()
                .entityName("TestEntity")
                .entityId("1")
                .operation(OperationType.UPDATE.name())
                .changes(Map.of("name", "test -> updated"))
                .build();

        when(logEntryFactory.create(entity, newEntity, OperationType.UPDATE))
                .thenReturn(expectedEntry);

        // when
        strategy.logChange(entity, newEntity, OperationType.UPDATE);

        // then
        BlockingQueue<LogEntry> logQueue = (BlockingQueue<LogEntry>) ReflectionTestUtils.getField(strategy, "logQueue");
        assertThat(logQueue).isNotEmpty();
        LogEntry entry = logQueue.poll();
        assertThat(entry).isEqualTo(expectedEntry);
    }

    @Test
    void logChange_ShouldNotLogWhenConditionIsFalse() {
        // given
        TestEntity entity = new TestEntity(1L, "test");
        TestEntity newEntity = new TestEntity(1L, "updated");
        when(loggingCondition.shouldLogChanges(any())).thenReturn(false);

        // when
        strategy.logChange(entity, newEntity, OperationType.UPDATE);

        // then
        BlockingQueue<LogEntry> logQueue = (BlockingQueue<LogEntry>) ReflectionTestUtils.getField(strategy, "logQueue");
        assertThat(logQueue).isEmpty();
    }

    @Test
    void processLogEntry_ShouldAddToBatchQueue() {
        // given
        LogEntry entry = LogEntry.builder()
                .entityName("TestEntity")
                .entityId("1")
                .operation(OperationType.INSERT.name())
                .build();

        // when
        ReflectionTestUtils.invokeMethod(strategy, "processLogEntry", entry);

        // then
        ConcurrentLinkedQueue<LogEntry> batchQueue = (ConcurrentLinkedQueue<LogEntry>) ReflectionTestUtils
                .getField(strategy, "batchQueue");
        assertThat(batchQueue).isNotEmpty();
        assertThat(batchQueue.poll()).isEqualTo(entry);
    }

    @Test
    void flushBatch_ShouldSaveToRepository() {
        // given
        List<LogEntry> entries = List.of(
                LogEntry.builder().entityName("Test1").entityId("1").operation(OperationType.INSERT.name()).build(),
                LogEntry.builder().entityName("Test2").entityId("2").operation(OperationType.UPDATE.name()).build());
        ConcurrentLinkedQueue<LogEntry> batchQueue = (ConcurrentLinkedQueue<LogEntry>) ReflectionTestUtils
                .getField(strategy, "batchQueue");
        entries.forEach(batchQueue::add);

        // when
        ReflectionTestUtils.invokeMethod(strategy, "flushBatch");

        // then
        verify(logEntryRepository).saveAll(entries);
        assertThat(batchQueue).isEmpty();
    }

    @Test
    void getEntityId_ShouldExtractIdFromEntity() {
        // given
        TestEntity entity = new TestEntity(1L, "test");

        // when
        String entityId = (String) ReflectionTestUtils.invokeMethod(strategy, "getEntityId", entity, null);

        // then
        assertThat(entityId).isEqualTo("1");
    }

    @Test
    void getEntityId_ShouldHandleNullId() {
        // given
        TestEntity entity = new TestEntity(null, "test");

        // when
        String entityId = (String) ReflectionTestUtils.invokeMethod(strategy, "getEntityId", entity, null);

        // then
        assertThat(entityId).isEqualTo("null");
    }

    @Test
    void getEntityId_ShouldHandleAccessError() throws Exception {
        // given
        TestEntityWithPrivateId entity = new TestEntityWithPrivateId(1L);

        // id 필드의 접근성을 더 엄격하게 제한
        Field idField = TestEntityWithPrivateId.class.getDeclaredField("id");
        idField.setAccessible(false); // 명시적으로 접근 불가능하게 설정

        // when
        String entityId = (String) ReflectionTestUtils.invokeMethod(strategy, "getEntityId", entity, null);

        // then
        assertThat(entityId).isEqualTo("Access Error");
    }

    @Test
    void logChange_ShouldHandleNullEntities() {
        // when
        strategy.logChange(null, null, OperationType.DELETE);

        // then
        BlockingQueue<LogEntry> logQueue = (BlockingQueue<LogEntry>) ReflectionTestUtils.getField(strategy, "logQueue");
        assertThat(logQueue).isEmpty();
    }

    @Test
    void logChange_ShouldHandleQueueFull() throws InterruptedException {
        // given
        // 기존 큐와 프로세서 제거
        ExecutorService logProcessorPool = (ExecutorService) ReflectionTestUtils.getField(strategy, "logProcessorPool");
        logProcessorPool.shutdownNow();
        Thread.sleep(100); // 프로세서가 완전히 종료될 때까지 대기

        // 새로운 큐 설정
        BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(1);
        ReflectionTestUtils.setField(strategy, "logQueue", logQueue);

        // 큐를 가득 채움
        LogEntry existingEntry = LogEntry.builder()
                .entityName("Existing")
                .entityId("0")
                .operation(OperationType.INSERT.name())
                .build();

        // 큐가 비어있는지 확인
        assertThat(logQueue.isEmpty()).isTrue();

        // 큐에 엔트리 추가
        boolean added = logQueue.offer(existingEntry);
        assertThat(added).isTrue(); // 큐에 성공적으로 추가되었는지 확인
        assertThat(logQueue.size()).isEqualTo(1); // 큐가 가득 찼는지 확인

        // when
        TestEntity entity = new TestEntity(1L, "test");
        strategy.logChange(entity, null, OperationType.DELETE);

        // then
        assertThat(logQueue.size()).isEqualTo(1); // 큐가 가득 차서 새로운 엔트리가 추가되지 않음
        LogEntry peekedEntry = logQueue.peek();
        assertThat(peekedEntry).isNotNull(); // peek한 엔트리가 null이 아님
        assertThat(peekedEntry).isEqualTo(existingEntry); // 기존 엔트리가 그대로 남아있는지 확인
    }

    @Test
    void flushBatch_ShouldHandleRepositoryError() {
        // given
        List<LogEntry> entries = List.of(
                LogEntry.builder().entityName("Test1").entityId("1").operation(OperationType.INSERT.name()).build());
        ConcurrentLinkedQueue<LogEntry> batchQueue = (ConcurrentLinkedQueue<LogEntry>) ReflectionTestUtils
                .getField(strategy, "batchQueue");
        entries.forEach(batchQueue::add);

        doThrow(new DataAccessException("Database error") {
        }).when(logEntryRepository).saveAll(entries);

        // when
        ReflectionTestUtils.invokeMethod(strategy, "flushBatch");

        // then
        assertThat(batchQueue).isEmpty(); // 에러가 발생해도 배치 큐는 비워짐
    }

    @Test
    void getEntityId_ShouldHandleNonEntityObject() {
        // given
        Object nonEntity = new Object();

        // when
        String entityId = (String) ReflectionTestUtils.invokeMethod(strategy, "getEntityId", nonEntity, null);

        // then
        assertThat(entityId).isEqualTo("Unknown");
    }

    @Test
    void getEntityId_ShouldHandleEntityWithoutId() {
        // given
        EntityWithoutId entity = new EntityWithoutId("test");

        // when
        String entityId = (String) ReflectionTestUtils.invokeMethod(strategy, "getEntityId", entity, null);

        // then
        assertThat(entityId).isEqualTo("Unknown");
    }

    @Test
    void processLogEntry_ShouldHandleNullEntry() {
        // when
        ReflectionTestUtils.invokeMethod(strategy, "processLogEntry", (LogEntry) null);

        // then
        ConcurrentLinkedQueue<LogEntry> batchQueue = (ConcurrentLinkedQueue<LogEntry>) ReflectionTestUtils
                .getField(strategy, "batchQueue");
        assertThat(batchQueue).isEmpty();
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

    private static class TestEntityWithPrivateId {
        @Id
        private final Long id;

        private TestEntityWithPrivateId(Long id) {
            this.id = id;
        }
    }

    private static class EntityWithoutId {
        private String name;

        public EntityWithoutId(String name) {
            this.name = name;
        }
    }
}