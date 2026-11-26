package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ProductHistoryItem;
import java.util.List;

public interface InventoryService {

    /** Products with QuantityInStock <= MinStock (active only). */
    List<LowStockItem> lowStock();

    /**
     * Stock-in / stock-out history of one product, from CONFIRMED documents only, newest first, at most the 500
     * latest movements. 404 if the product does not exist.
     */
    List<ProductHistoryItem> history(Long productId);
}
