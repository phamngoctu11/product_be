package com.example.workflow.service;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.exception.AppException; // Import class lỗi tùy chỉnh
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor

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
    @Cacheable(value = "products")
    public List<ProductDTO> getAllProducts() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not found with id: " + id));
        return mapper.toDto(product);
    }
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void updateProduct(Long id, ProductDTO dto) {
        Product existingProduct = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Cannot update. Product not found with id: " + id));
        mapper.updateProductFromDto(dto, existingProduct);
        mapper.toDto(existingProduct);
    }
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void deleteProduct(Long id) {
        if (!repository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Cannot delete. Product not found with id: " + id);
        }
        repository.deleteById(id);
    }
}