package com.pandaterry.concurrent_entity_change_logger.loadtest.controller;

import com.pandaterry.concurrent_entity_change_logger.loadtest.dto.OrderDto;
import com.pandaterry.concurrent_entity_change_logger.loadtest.entity.OrderStatus;
import com.pandaterry.concurrent_entity_change_logger.loadtest.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/test/orders")
@RequiredArgsConstructor
public class TestOrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto orderDto) {
        return ResponseEntity.ok(orderService.createOrder(orderDto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    @PutMapping("/{id}/amount")
    public ResponseEntity<OrderDto> updateAmount(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(orderService.updateAmount(id, amount));
    }
}