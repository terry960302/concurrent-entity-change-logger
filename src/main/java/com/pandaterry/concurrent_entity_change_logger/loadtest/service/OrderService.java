package com.pandaterry.concurrent_entity_change_logger.loadtest.service;

import com.pandaterry.concurrent_entity_change_logger.loadtest.dto.OrderDto;
import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.OrderStatus;
import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.TestOrder;
import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.TestUser;
import com.pandaterry.concurrent_entity_change_logger.loadtest.repository.TestOrderRepository;
import com.pandaterry.concurrent_entity_change_logger.loadtest.repository.TestUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final TestOrderRepository orderRepository;
    private final TestUserRepository userRepository;

    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        TestUser user = userRepository.findById(orderDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TestOrder order = TestOrder.builder()
                .orderNumber(orderDto.getOrderNumber())
                .totalAmount(orderDto.getTotalAmount())
                .user(user)
                .build();
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto updateStatus(Long id, OrderStatus status) {
        TestOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.updateStatus(status);
        return convertToDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto updateAmount(Long id, BigDecimal amount) {
        TestOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.updateAmount(amount);
        return convertToDto(orderRepository.save(order));
    }

    private OrderDto convertToDto(TestOrder order) {
        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .orderedAt(order.getOrderedAt())
                .updatedAt(order.getUpdatedAt())
                .userId(order.getUser().getId())
                .build();
    }
}