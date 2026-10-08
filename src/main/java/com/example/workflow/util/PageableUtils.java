package com.example.workflow.util;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@UtilityClass
public class PageableUtils {

    public Pageable normalize(Pageable pageable, int defaultSize, int maxSize) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int requestedSize = pageable == null ? defaultSize : pageable.getPageSize();
        int size = Math.min(Math.max(requestedSize, 1), maxSize);
        return PageRequest.of(page, size);
    }
}
