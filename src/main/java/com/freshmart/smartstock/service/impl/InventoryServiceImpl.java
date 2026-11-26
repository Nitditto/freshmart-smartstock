package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.enums.MovementDirection;
import com.freshmart.smartstock.domain.enums.MovementDocumentType;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ProductHistoryItem;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueDetailRepository;
import com.freshmart.smartstock.repository.StockReceiptDetailRepository;
import com.freshmart.smartstock.service.InventoryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    /** Upper bound of GET /api/products/{id}/history; older movements are not returned. */
    public static final int MAX_HISTORY_ITEMS = 500;

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
        // Each side is capped first, so a busy product never loads its whole life; the merged list is capped again.
        Pageable newest = PageRequest.of(0, MAX_HISTORY_ITEMS);
        List<ProductHistoryItem> items = new ArrayList<>();
        receiptDetailRepository.findConfirmedByProductId(productId, newest).forEach(detail -> items.add(
                new ProductHistoryItem(detail.getReceipt().getConfirmedAt(), MovementDirection.IN,
                        MovementDocumentType.RECEIPT, detail.getReceipt().getCode(), detail.getQuantity())));
        issueDetailRepository.findConfirmedByProductId(productId, newest).forEach(detail -> items.add(
                new ProductHistoryItem(detail.getIssue().getConfirmedAt(), MovementDirection.OUT,
                        MovementDocumentType.ISSUE, detail.getIssue().getCode(), detail.getQuantity())));

        // A CONFIRMED row without confirmed_at (the schema allows it) must not break the whole list: it goes last.
        Comparator<Instant> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        items.sort(Comparator.comparing(ProductHistoryItem::occurredAt, nullsFirst).reversed());
        return items.stream().limit(MAX_HISTORY_ITEMS).toList();
    }
}
