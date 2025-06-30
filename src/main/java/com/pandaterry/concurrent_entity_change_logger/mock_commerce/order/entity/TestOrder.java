package com.pandaterry.concurrent_entity_change_logger.mock_commerce.order.entity;

import com.pandaterry.concurrent_entity_change_logger.mock_commerce.order.entity.OrderStatus;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.entity.TestUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_order")
@Getter
@Setter
@NoArgsConstructor
public class TestOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column
    private LocalDateTime orderedAt;

    @Column
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private TestUser user;

    @Builder
    public TestOrder(String orderNumber, BigDecimal totalAmount, TestUser user) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.user = user;
        this.status = OrderStatus.CREATED;
        this.orderedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAmount(BigDecimal newAmount) {
        this.totalAmount = newAmount;
        this.updatedAt = LocalDateTime.now();
    }
}
