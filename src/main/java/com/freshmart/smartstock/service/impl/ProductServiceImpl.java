package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.api.LikePattern;
import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.api.PageableGuard;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Category;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.dto.product.ProductRequest;
import com.freshmart.smartstock.dto.product.ProductResponse;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.service.ProductService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Set<String> SORTABLE = Set.of("id", "sku", "name", "price", "quantityInStock");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Long categoryId, Pageable pageable) {
        Pageable safe = PageableGuard.sanitize(pageable, SORTABLE);
        String kw = (keyword == null || keyword.isBlank()) ? null : LikePattern.escape(keyword.trim());
        return PageResponse.from(productRepository.search(kw, categoryId, true, safe), ProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm " + id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim();
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw ApiException.conflict("Mã SKU '" + sku + "' đã tồn tại");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục " + request.categoryId()));

        Product product = new Product();
        product.setSku(sku);
        product.setName(request.name().trim());
        product.setCategory(category);
        product.setUnit(request.unit().trim());
        product.setPrice(request.price());
        product.setMinStock(request.minStock());
        product.setQuantityInStock(0);
        product.setActive(true);

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm " + id));

        String sku = request.sku().trim();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw ApiException.conflict("Mã SKU '" + sku + "' đã tồn tại");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục " + request.categoryId()));

        product.setSku(sku);
        product.setName(request.name().trim());
        product.setCategory(category);
        product.setUnit(request.unit().trim());
        product.setPrice(request.price());
        product.setMinStock(request.minStock());

        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public void hide(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm " + id));
        product.setActive(false);
    }
}
