package com.example.workflow.service;

import com.example.workflow.cache.CacheNames;
import com.example.workflow.dto.ProductTypeDTO;
import com.example.workflow.dto.ProductTypeFieldDTO;
import com.example.workflow.dto.SaveProductTypeRequest;
import com.example.workflow.entity.ProductType;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.ProductTypeFieldKind;
import com.example.workflow.repository.ProductTypeRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductTypeService {
    private static final TypeReference<List<ProductTypeFieldDTO>> FIELD_LIST = new TypeReference<>() {};
    private final ProductTypeRepository repository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final ApplicationCacheService applicationCacheService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PRODUCT_TYPES, key = "'active'")
    public List<ProductTypeDTO> listActive() {
        return repository.findAllByActiveTrueOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PRODUCT_TYPES, key = "'all'")
    public List<ProductTypeDTO> listAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PRODUCT_TYPES, key = "'id-' + #id")
    public ProductTypeDTO getActive(Long id) {
        ProductType type = repository.findById(id).filter(ProductType::isActive)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_TYPE_NOT_FOUND));
        return toDto(type);
    }

    @Transactional
    public ProductTypeDTO create(SaveProductTypeRequest request) {
        currentUserService.requireCurrentUserId();
        validate(request, false);
        if (repository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCT_TYPE_INVALID, "Mã loại đã được sử dụng.");
        }
        ProductType type = new ProductType();
        apply(type, request);
        ProductType saved = repository.saveAndFlush(type);
        applicationCacheService.evictCatalogTaxonomyChanged();
        return toDto(saved);
    }

    @Transactional
    public ProductTypeDTO update(Long id, SaveProductTypeRequest request) {
        currentUserService.requireCurrentUserId();
        validate(request, true);
        ProductType type = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_TYPE_NOT_FOUND));
        repository.findByCodeIgnoreCase(request.code().trim()).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCT_TYPE_INVALID, "Mã loại đã được sử dụng."); });
        apply(type, request);
        ProductType saved = repository.saveAndFlush(type);
        applicationCacheService.evictCatalogTaxonomyChanged();
        return toDto(saved);
    }

    @Transactional
    public ProductTypeDTO deactivate(Long id) {
        currentUserService.requireCurrentUserId();
        ProductType type = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_TYPE_NOT_FOUND));
        type.setActive(false);
        ProductType saved = repository.saveAndFlush(type);
        applicationCacheService.evictCatalogTaxonomyChanged();
        return toDto(saved);
    }

    private void validate(SaveProductTypeRequest request, boolean updating) {
        if (request == null || !StringUtils.hasText(request.code()) || !StringUtils.hasText(request.name())
                || request.fields() == null) {
            throw invalid("Thiếu mã, tên, danh sách trường hoặc phiên bản cập nhật.");
        }
        Set<String> codes = new HashSet<>();
        for (ProductTypeFieldDTO field : request.fields()) {
            if (field == null || !StringUtils.hasText(field.code()) || !StringUtils.hasText(field.label()) || field.kind() == null) {
                throw invalid("Mỗi trường cần có mã, nhãn và kiểu dữ liệu.");
            }
            if (!codes.add(field.code().trim().toLowerCase(Locale.ROOT))) throw invalid("Mã trường bị trùng: " + field.code());
            List<String> options = field.options() == null ? List.of() : field.options().stream()
                    .filter(StringUtils::hasText).map(String::trim).distinct().toList();
            if ((field.kind() == ProductTypeFieldKind.SINGLE_SELECT || field.kind() == ProductTypeFieldKind.MULTI_SELECT)
                    && options.isEmpty()) throw invalid("Trường chọn cần có ít nhất một lựa chọn: " + field.label());
            if (field.kind() != ProductTypeFieldKind.SINGLE_SELECT && field.kind() != ProductTypeFieldKind.MULTI_SELECT
                    && !options.isEmpty()) throw invalid("Chỉ trường chọn mới được khai báo danh sách lựa chọn: " + field.label());
            if (field.kind() == ProductTypeFieldKind.TEXT && field.maxLength() != null
                    && (field.maxLength() < 1 || field.maxLength() > 4000)) throw invalid("Độ dài tối đa không hợp lệ: " + field.label());
            if (field.minimum() != null && field.maximum() != null && field.minimum().compareTo(field.maximum()) > 0)
                throw invalid("Giá trị nhỏ nhất lớn hơn giá trị lớn nhất: " + field.label());
        }
    }

    private void apply(ProductType type, SaveProductTypeRequest request) {
        type.setCode(request.code().trim().toLowerCase(Locale.ROOT));
        type.setName(request.name().trim());
        type.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        type.setFieldsJson(writeFields(request.fields().stream().map(this::normalizeField).toList()));
        type.setActive(request.active());
    }

    private ProductTypeFieldDTO normalizeField(ProductTypeFieldDTO field) {
        return new ProductTypeFieldDTO(field.code().trim().toLowerCase(Locale.ROOT), field.label().trim(), field.kind(),
                field.displayOrder(), StringUtils.hasText(field.unit()) ? field.unit().trim() : null,
                StringUtils.hasText(field.hint()) ? field.hint().trim() : null, field.maxLength(), field.minimum(), field.maximum(),
                field.options() == null ? List.of() : field.options().stream().filter(StringUtils::hasText).map(String::trim).distinct().toList());
    }

    public List<ProductTypeFieldDTO> readFields(String json) {
        try {
            return objectMapper.readValue(json == null ? "[]" : json, FIELD_LIST);
        } catch (Exception exception) {
            throw invalid("Không thể đọc cấu hình trường đã lưu.");
        }
    }

    private String writeFields(List<ProductTypeFieldDTO> fields) {
        try {
            return objectMapper.writeValueAsString(fields.stream()
                    .sorted(java.util.Comparator.comparing(field -> field.displayOrder() == null ? 0 : field.displayOrder()))
                    .toList());
        } catch (Exception exception) {
            throw invalid("Không thể lưu cấu hình trường.");
        }
    }

    private ProductTypeDTO toDto(ProductType type) {
        return new ProductTypeDTO(type.getId(), type.getCode(), type.getName(), type.getDescription(),
                readFields(type.getFieldsJson()), type.isActive());
    }

    private AppException invalid(String detail) {
        return new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_TYPE_INVALID, detail);
    }
}
