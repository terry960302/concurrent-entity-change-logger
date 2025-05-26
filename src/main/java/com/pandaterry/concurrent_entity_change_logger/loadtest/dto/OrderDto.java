package com.pandaterry.concurrent_entity_change_logger.loadtest.dto;

import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;
    private Long userId;
}