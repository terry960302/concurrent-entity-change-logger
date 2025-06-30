package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.mapper;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.entity.LogEntryJpo;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.out.persistence.mapper.LogEntryMapper;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.model.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.ChangeSet;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.EntityIdentifier;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.FieldChange;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.LogEntryId;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo.SubmissionContext;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.order.entity.OrderStatus;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.order.entity.TestOrder;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.entity.TestProduct;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.entity.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LogEntryMapperTest {

    private LogEntryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LogEntryMapper();
    }

    @Test
    @DisplayName("Order 상태 변경 로그 매핑 테스트")
    void shouldMapOrderStatusChange() {
        TestUser user = createTestUser();
        TestOrder order = createTestOrder(user);
        EntityIdentifier entityId = EntityIdentifier.fromEntity(order);

        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("status", FieldChange.of("status", "CREATED", "PAID"));
        changes.put("updatedAt", FieldChange.of("updatedAt", "2024-01-01T10:00:00", "2024-01-01T11:00:00"));
        ChangeSet changeSet = ChangeSet.of(changes);

        LogEntry logEntry = LogEntry.create(entityId, changeSet, Operation.UPDATE);
        LogEntryJpo jpo = mapper.toEntity(logEntry);
        LogEntry restored = mapper.toDomain(jpo);

        assertThat(restored).isEqualTo(logEntry);
        assertThat(restored.getEntityIdentifier().getEntityName()).isEqualTo("TestOrder");
        assertThat(restored.getOperation()).isEqualTo(Operation.UPDATE);
        assertThat(restored.getChangeSet().getChangeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Product 가격 변경 로그 매핑 테스트")
    void shouldMapProductPriceChange() {
        // Given: 실제 Product 객체 생성
        TestProduct product = createTestProduct();
        EntityIdentifier entityId = EntityIdentifier.fromEntity(product);

        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("price", FieldChange.of("price", "5000", "6000"));
        changes.put("updatedAt", FieldChange.of("updatedAt", "2024-01-01T10:00:00", "2024-01-01T11:00:00"));
        ChangeSet changeSet = ChangeSet.of(changes);

        LogEntry logEntry = LogEntry.create(entityId, changeSet, Operation.UPDATE);
        LogEntryJpo jpo = mapper.toEntity(logEntry);
        LogEntry restored = mapper.toDomain(jpo);

        // Then: 매핑 검증
        assertThat(restored).isEqualTo(logEntry);
        assertThat(restored.getEntityIdentifier().getEntityName()).isEqualTo("TestProduct");
        assertThat(restored.getOperation()).isEqualTo(Operation.UPDATE);
    }

    @Test
    @DisplayName("User 이메일 변경 로그 매핑 테스트")
    void shouldMapUserEmailChange() {
        TestUser user = createTestUser();
        EntityIdentifier entityId = EntityIdentifier.fromEntity(user);

        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("email", FieldChange.of("email", "old@example.com", "new@example.com"));
        ChangeSet changeSet = ChangeSet.of(changes);

        LogEntry logEntry = LogEntry.create(entityId, changeSet, Operation.UPDATE);
        LogEntryJpo jpo = mapper.toEntity(logEntry);
        LogEntry restored = mapper.toDomain(jpo);

        assertThat(restored).isEqualTo(logEntry);
        assertThat(restored.getEntityIdentifier().getEntityName()).isEqualTo("TestUser");
        assertThat(restored.getOperation()).isEqualTo(Operation.UPDATE);
    }

    @Test
    @DisplayName("CREATE, UPDATE, DELETE Operation 매핑 테스트")
    void shouldMapAllOperationTypes() {
        TestUser user = createTestUser();
        EntityIdentifier entityId = EntityIdentifier.fromEntity(user);
        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("name", FieldChange.of("name", "홍길동", "김철수"));
        ChangeSet changeSet = ChangeSet.of(changes);

        LogEntry createEntry = LogEntry.create(entityId, changeSet, Operation.CREATE);
        LogEntryJpo createJpo = mapper.toEntity(createEntry);
        LogEntry restoredCreate = mapper.toDomain(createJpo);
        assertThat(restoredCreate).isEqualTo(createEntry);
        assertThat(restoredCreate.getOperation()).isEqualTo(Operation.CREATE);

        LogEntry updateEntry = LogEntry.create(entityId, changeSet, Operation.UPDATE);
        LogEntryJpo updateJpo = mapper.toEntity(updateEntry);
        LogEntry restoredUpdate = mapper.toDomain(updateJpo);
        assertThat(restoredUpdate).isEqualTo(updateEntry);
        assertThat(restoredUpdate.getOperation()).isEqualTo(Operation.UPDATE);

        LogEntry deleteEntry = LogEntry.create(entityId, changeSet, Operation.DELETE);
        LogEntryJpo deleteJpo = mapper.toEntity(deleteEntry);
        LogEntry restoredDelete = mapper.toDomain(deleteJpo);
        assertThat(restoredDelete).isEqualTo(deleteEntry);
        assertThat(restoredDelete.getOperation()).isEqualTo(Operation.DELETE);
    }

    @Test
    @DisplayName("여러 필드 동시 변경 매핑 테스트")
    void shouldMapMultipleFieldChanges() {
        TestProduct product = createTestProduct();
        EntityIdentifier entityId = EntityIdentifier.fromEntity(product);

        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("price", FieldChange.of("price", "5000", "6000"));
        changes.put("stockQuantity", FieldChange.of("stockQuantity", "100", "80"));
        changes.put("active", FieldChange.of("active", "true", "false"));
        changes.put("updatedAt", FieldChange.of("updatedAt", "2024-01-01T10:00:00", "2024-01-01T11:00:00"));
        ChangeSet changeSet = ChangeSet.of(changes);

        LogEntry logEntry = LogEntry.create(entityId, changeSet, Operation.UPDATE);
        LogEntryJpo jpo = mapper.toEntity(logEntry);
        LogEntry restored = mapper.toDomain(jpo);

        assertThat(restored).isEqualTo(logEntry);
        assertThat(restored.getChangeSet().getChangeCount()).isEqualTo(4);
        assertThat(restored.getChangeSet().hasAnyChanges()).isTrue();
        assertThat(restored.getEntityIdentifier().getEntityName()).isEqualTo("TestProduct");
        assertThat(restored.getOperation()).isEqualTo(Operation.UPDATE);
    }

    @Test
    @DisplayName("null 값 처리 매핑 테스트")
    void shouldMapNullValues() {
        TestUser user = createTestUser();
        EntityIdentifier entityId = EntityIdentifier.fromEntity(user);

        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("phoneNumber", FieldChange.of("phoneNumber", "010-1234-5678", null));
        changes.put("lastLoginAt", FieldChange.of("lastLoginAt", null, "2024-01-01T10:00:00"));
        ChangeSet changeSet = ChangeSet.of(changes);

        LogEntry logEntry = LogEntry.create(entityId, changeSet, Operation.UPDATE);
        LogEntryJpo jpo = mapper.toEntity(logEntry);
        LogEntry restored = mapper.toDomain(jpo);

        assertThat(restored).isEqualTo(logEntry);
        assertThat(restored.getChangeSet().getChangeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("빈 ChangeSet 매핑 테스트")
    void shouldMapEmptyChangeSet() {
        TestOrder order = createTestOrder(createTestUser());
        EntityIdentifier entityId = EntityIdentifier.fromEntity(order);
        ChangeSet emptyChangeSet = ChangeSet.empty();

        LogEntry logEntry = LogEntry.create(entityId, emptyChangeSet, Operation.CREATE);
        LogEntryJpo jpo = mapper.toEntity(logEntry);
        LogEntry restored = mapper.toDomain(jpo);

        assertThat(restored).isEqualTo(logEntry);
        assertThat(restored.getChangeSet().getChangeCount()).isEqualTo(0);
        assertThat(restored.getChangeSet().hasAnyChanges()).isFalse();
    }

    @Test
    @DisplayName("양방향 매핑 무결성 테스트")
    void shouldMaintainDataIntegrityInBidirectionalMapping() {
        TestProduct product = createTestProduct();
        EntityIdentifier entityId = EntityIdentifier.fromEntity(product);

        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("name", FieldChange.of("name", "기존 상품명", "새로운 상품명"));
        changes.put("price", FieldChange.of("price", "5000", "6000"));
        changes.put("description", FieldChange.of("description", "기존 설명", "새로운 설명"));
        changes.put("stockQuantity", FieldChange.of("stockQuantity", "100", "80"));
        changes.put("active", FieldChange.of("active", "true", "false"));
        ChangeSet changeSet = ChangeSet.of(changes);

        LogEntry original = LogEntry.create(entityId, changeSet, Operation.UPDATE);

        LogEntryJpo jpo = mapper.toEntity(original);
        LogEntry restored = mapper.toDomain(jpo);

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getEntityIdentifier()).isEqualTo(original.getEntityIdentifier());
        assertThat(restored.getOperation()).isEqualTo(original.getOperation());
        assertThat(restored.getRecordedAt()).isEqualTo(original.getRecordedAt());

        assertThat(restored.getChangeSet().getChangeCount()).isEqualTo(original.getChangeSet().getChangeCount());
        assertThat(restored.getChangeSet().hasAnyChanges()).isEqualTo(original.getChangeSet().hasAnyChanges());
        assertThat(restored.getContext().getThreadName()).isEqualTo(original.getContext().getThreadName());
    }

    @Test
    @DisplayName("LogEntryJpo에서 LogEntry 복원 테스트")
    void shouldRestoreLogEntryFromJpo() {
        UUID id = UUID.randomUUID();
        Instant recordedAt = Instant.now();

        Map<String, Object> changeData = new HashMap<>();
        changeData.put("fieldName", "price");
        changeData.put("oldValue", "5000");
        changeData.put("newValue", "6000");
        changeData.put("changeType", "MODIFIED");

        Map<String, Object> changes = new HashMap<>();
        changes.put("price", changeData);

        Map<String, Object> context = new HashMap<>();
        context.put("threadName", "test-thread");
        context.put("hostname", "test-host");
        context.put("submittedAt", recordedAt.toString());

        LogEntryJpo jpo = LogEntryJpo.builder()
                .id(id)
                .entityName("TestProduct")
                .entityId("1")
                .changes(changes)
                .operation(Operation.UPDATE)
                .recordedAt(recordedAt)
                .context(context)
                .build();

        LogEntry restored = mapper.toDomain(jpo);

        assertThat(restored.getId().getValue()).isEqualTo(id);
        assertThat(restored.getEntityIdentifier().getEntityName()).isEqualTo("TestProduct");
        assertThat(restored.getEntityIdentifier().getEntityId()).isEqualTo("1");
        assertThat(restored.getOperation()).isEqualTo(Operation.UPDATE);
        assertThat(restored.getRecordedAt()).isEqualTo(recordedAt);
        assertThat(restored.getChangeSet().getChangeCount()).isEqualTo(1);
        assertThat(restored.getContext().getThreadName()).isEqualTo("test-thread");
        assertThat(restored.getContext().toMap()).containsEntry("hostname", "test-host");
    }

    // Helper 메서드들
    private TestUser createTestUser() {
        return TestUser.builder()
                .name("테스트 사용자")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .build();
    }

    private TestOrder createTestOrder(TestUser user) {
        return TestOrder.builder()
                .orderNumber("ORD-001")
                .totalAmount(new BigDecimal("10000"))
                .user(user)
                .build();
    }

    private TestProduct createTestProduct() {
        return TestProduct.builder()
                .name("테스트 상품")
                .price(new BigDecimal("5000"))
                .description("테스트 상품 설명")
                .stockQuantity(100)
                .build();
    }
}