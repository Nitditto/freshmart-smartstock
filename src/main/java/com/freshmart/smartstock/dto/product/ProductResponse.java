package com.freshmart.smartstock.dto.product;

import com.freshmart.smartstock.domain.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        Long categoryId,
        String categoryName,
        String unit,
        BigDecimal price,
        int quantityInStock,
        int minStock,
        boolean active) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getUnit(), p.getPrice(), p.getQuantityInStock(), p.getMinStock(), p.isActive());
    }
}
