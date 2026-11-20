package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.product.ProductRequest;
import com.freshmart.smartstock.dto.product.ProductResponse;
import com.freshmart.smartstock.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * TODO[Member B]: implement with ProductRepository.search(...) and CategoryRepository.
 * Whitelist sortable fields (id, sku, name, price, quantityInStock) so clients cannot sort by arbitrary properties.
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Override
    public PageResponse<ProductResponse> search(String keyword, Long categoryId, Pageable pageable) {
        throw ApiException.notImplemented("Member B");
    }

    @Override
    public ProductResponse get(Long id) {
        throw ApiException.notImplemented("Member B");
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        throw ApiException.notImplemented("Member B");
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        throw ApiException.notImplemented("Member B");
    }

    @Override
    public void hide(Long id) {
        throw ApiException.notImplemented("Member B");
    }
}
