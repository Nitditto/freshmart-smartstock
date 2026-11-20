package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ProductHistoryItem;
import com.freshmart.smartstock.service.InventoryService;
import java.util.List;
import org.springframework.stereotype.Service;

/** TODO[Member B]: lowStock() -> ProductRepository.findLowStock(); history() -> new queries on the two detail repos. */
@Service
public class InventoryServiceImpl implements InventoryService {

    @Override
    public List<LowStockItem> lowStock() {
        throw ApiException.notImplemented("Member B");
    }

    @Override
    public List<ProductHistoryItem> history(Long productId) {
        throw ApiException.notImplemented("Member B");
    }
}
