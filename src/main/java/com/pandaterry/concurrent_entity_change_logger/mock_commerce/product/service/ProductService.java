package com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.service;

import com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.dto.ProductDto;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.entity.TestProduct;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.product.repository.TestProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final TestProductRepository productRepository;

    @Transactional
    public ProductDto createProduct(ProductDto productDto) {
        TestProduct product = TestProduct.builder()
                .name(productDto.getName())
                .price(productDto.getPrice())
                .description(productDto.getDescription())
                .stockQuantity(productDto.getStockQuantity())
                .build();
        return convertToDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateStock(Long id, int quantity) {
        TestProduct product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.updateStock(quantity);
        return convertToDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto activateProduct(Long id) {
        TestProduct product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.activate();
        return convertToDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto deactivateProduct(Long id) {
        TestProduct product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.deactivate();
        return convertToDto(productRepository.save(product));
    }

    private ProductDto convertToDto(TestProduct product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .stockQuantity(product.getStockQuantity())
                .active(product.isActive())
                .build();
    }
}