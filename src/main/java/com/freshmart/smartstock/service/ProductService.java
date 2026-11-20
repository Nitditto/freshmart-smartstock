package com.freshmart.smartstock.service;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.dto.product.ProductRequest;
import com.freshmart.smartstock.dto.product.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    /** Paged + sorted + keyword (name/SKU) + optional categoryId. Hidden products are excluded. */
    PageResponse<ProductResponse> search(String keyword, Long categoryId, Pageable pageable);

    /** 404 if missing. Includes current stock. */
    ProductResponse get(Long id);

    /** SKU unique (409), price >= 0, minStock >= 0, category must exist (404/400). */
    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    /** Soft delete: sets active=false; the product stays referenced by historical documents. */
    void hide(Long id);
}
