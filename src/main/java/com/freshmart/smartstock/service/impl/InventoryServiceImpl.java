package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ProductHistoryItem;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.service.InventoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LowStockItem> lowStock() {
        return productRepository.findLowStock().stream().map(LowStockItem::from).toList();
    }

    @Override
    public List<ProductHistoryItem> history(Long productId) {
        throw ApiException.notImplemented("Member B");
    }
}
