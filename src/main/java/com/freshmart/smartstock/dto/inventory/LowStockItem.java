package com.freshmart.smartstock.dto.inventory;

import com.freshmart.smartstock.domain.entity.Product;

public record LowStockItem(Long productId, String sku, String name, String unit, int quantityInStock, int minStock) {

    public static LowStockItem from(Product p) {
        return new LowStockItem(p.getId(), p.getSku(), p.getName(), p.getUnit(), p.getQuantityInStock(),
                p.getMinStock());
    }
}
