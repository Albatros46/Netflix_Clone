package com.NetflixClone.util;

import com.NetflixClone.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

public class PaginationUtils {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_BY = "createdAt";

    private PaginationUtils() {}

    public static Pageable createPageRequest(int page, int size, String sortBy, String sortDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = size > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : Math.max(size, 1);
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? DEFAULT_SORT_BY : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    public static Pageable createPageRequest(int page, int size, String sortBy) {
        return createPageRequest(page, size, sortBy, "desc");
    }

    public static Pageable createPageRequest(int page, int size) {
        return createPageRequest(page, size, DEFAULT_SORT_BY, "desc");
    }

    public static <T, R> PageResponse<R> toPageResponse(Page<T> page, Function<T, R> mapper) {
        List<R> content = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }

    public static <R> PageResponse<R> toPageResponse(Page<?> page, List<R> mappedContent) {
        return new PageResponse<>(mappedContent, page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}