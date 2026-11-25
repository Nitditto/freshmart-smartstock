package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.dto.product.ProductRequest;
import com.freshmart.smartstock.dto.product.ProductResponse;
import com.freshmart.smartstock.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search products (pagination, sorting, keyword on name/SKU, categoryId filter)")
    public ApiResponse<PageResponse<ProductResponse>> search(
            @Parameter(description = "Từ khóa tìm kiếm theo tên hoặc mã SKU")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "ID danh mục để lọc sản phẩm")
            @RequestParam(required = false) Long categoryId,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.ok(productService.search(keyword, categoryId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one product with its current stock")
    public ApiResponse<ProductResponse> get(@Parameter(description = "ID của sản phẩm") @PathVariable Long id) {
        return ApiResponse.ok(productService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product (ADMIN)")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.created(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product (ADMIN)")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hide a product - soft delete (ADMIN)")
    public ApiResponse<Void> hide(@PathVariable Long id) {
        productService.hide(id);
        return ApiResponse.ok(null);
    }
}
