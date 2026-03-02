package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO createProduct(ProductDTO dto) {
        Product entity = mapper.toEntity(dto);
        Product savedProduct = repository.save(entity);
        return mapper.toDto(savedProduct);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not found with id: " + id));
        return mapper.toDto(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void updateProduct(Long id, ProductDTO dto) {
        Product existingProduct = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Cannot update. Product not found with id: " + id));
        mapper.updateProductFromDto(dto, existingProduct);
        mapper.toDto(existingProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(Long id) {
        if (!repository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Cannot delete. Product not found with id: " + id);
        }
        repository.deleteById(id);
    }
}