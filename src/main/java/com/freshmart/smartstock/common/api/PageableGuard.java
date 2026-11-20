package com.freshmart.smartstock.common.api;

import com.freshmart.smartstock.common.exception.ApiException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Whitelists sortable properties and caps the page size, so a client cannot sort by an arbitrary
 * property (500 error / information leak) or request a million rows. Used by every list endpoint.
 */
public final class PageableGuard {

    public static final int MAX_PAGE_SIZE = 100;

    private PageableGuard() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSortProperties) {
        if (pageable.isUnpaged()) {
            return PageRequest.of(0, MAX_PAGE_SIZE);
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("size tối đa là " + MAX_PAGE_SIZE);
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSortProperties.contains(order.getProperty())) {
                throw ApiException.badRequest("Không thể sắp xếp theo '" + order.getProperty()
                        + "'. Cho phép: " + allowedSortProperties);
            }
        }
        return pageable;
    }
}
