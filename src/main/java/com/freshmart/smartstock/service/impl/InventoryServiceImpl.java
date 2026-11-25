package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ProductHistoryItem;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueDetailRepository;
import com.freshmart.smartstock.repository.StockReceiptDetailRepository;
import com.freshmart.smartstock.service.InventoryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final StockReceiptDetailRepository receiptDetailRepository;
    private final StockIssueDetailRepository issueDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LowStockItem> lowStock() {
        return productRepository.findLowStock().stream().map(LowStockItem::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductHistoryItem> history(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw ApiException.notFound("Không tìm thấy sản phẩm " + productId);
        }

        List<ProductHistoryItem> items = new ArrayList<>();

        receiptDetailRepository.findConfirmedByProductId(productId).forEach(detail ->
                items.add(new ProductHistoryItem(
                        detail.getReceipt().getConfirmedAt(),
                        "IN",
                        "RECEIPT",
                        detail.getReceipt().getCode(),
                        detail.getQuantity()
                ))
        );

        issueDetailRepository.findConfirmedByProductId(productId).forEach(detail ->
                items.add(new ProductHistoryItem(
                        detail.getIssue().getConfirmedAt(),
                        "OUT",
                        "ISSUE",
                        detail.getIssue().getCode(),
                        detail.getQuantity()
                ))
        );

        items.sort(Comparator.comparing(ProductHistoryItem::occurredAt).reversed());
        return items;
    }
}
