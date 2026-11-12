package com.aptisprep.mvc.support.pagination;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Shared pagination helper for MVC controllers.
 *
 * <p>This is application support code, not a controller. Keeping it outside
 * the web layer prevents controllers from becoming a home for reusable logic.
 */
public final class ListPagination {
    private static final int PAGE_SIZE = 12;

    private ListPagination() {}

    public static <T> Page<T> slice(List<T> items, int requestedPage) {
        int lastPage = Math.max(0, (items.size() - 1) / PAGE_SIZE);
        int page = Math.clamp(requestedPage, 0, lastPage);
        int start = page * PAGE_SIZE;
        int end = start + Math.min(PAGE_SIZE, items.size() - start);
        return new PageImpl<>(
                items.subList(start, end), PageRequest.of(page, PAGE_SIZE), items.size());
    }
}
