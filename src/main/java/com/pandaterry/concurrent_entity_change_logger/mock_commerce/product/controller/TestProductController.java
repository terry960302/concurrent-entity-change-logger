package com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.controller;

import com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.dto.ProductDto;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/products")
@RequiredArgsConstructor
public class TestProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.createProduct(productDto));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ProductDto> activateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.activateProduct(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ProductDto> deactivateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deactivateProduct(id));
    }
}