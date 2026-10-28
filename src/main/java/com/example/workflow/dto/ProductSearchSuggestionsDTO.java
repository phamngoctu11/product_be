package com.example.workflow.dto;

import java.util.List;

public record ProductSearchSuggestionsDTO(
        List<ProductTypeSearchSuggestionDTO> productTypes,
        List<ProductDTO> products,
        long totalProducts
) {
}
