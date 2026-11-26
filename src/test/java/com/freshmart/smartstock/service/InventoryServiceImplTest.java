package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.entity.StockIssueDetail;
import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import com.freshmart.smartstock.domain.enums.MovementDirection;
import com.freshmart.smartstock.domain.enums.MovementDocumentType;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ProductHistoryItem;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueDetailRepository;
import com.freshmart.smartstock.repository.StockReceiptDetailRepository;
import com.freshmart.smartstock.service.impl.InventoryServiceImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReceiptDetailRepository receiptDetailRepository;

    @Mock
    private StockIssueDetailRepository issueDetailRepository;

    @InjectMocks
    private InventoryServiceImpl service;

    @Test
    void lowStockMapsProductsToLowStockItemsCorrectly() {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setSku("RC-001");
        p1.setName("Cà chua bi");
        p1.setUnit("hộp");
        p1.setQuantityInStock(2);
        p1.setMinStock(10);
        p1.setActive(true);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setSku("TT-005");
        p2.setName("Thịt thăn bò Úc");
        p2.setUnit("khay");
        p2.setQuantityInStock(4);
        p2.setMinStock(5);
        p2.setActive(true);

        when(productRepository.findLowStock()).thenReturn(List.of(p1, p2));

        List<LowStockItem> result = service.lowStock();

        assertEquals(2, result.size());

        LowStockItem item1 = result.get(0);
        assertEquals(1L, item1.productId());
        assertEquals("RC-001", item1.sku());
        assertEquals("Cà chua bi", item1.name());
        assertEquals("hộp", item1.unit());
        assertEquals(2, item1.quantityInStock());
        assertEquals(10, item1.minStock());

        LowStockItem item2 = result.get(1);
        assertEquals(2L, item2.productId());
        assertEquals("TT-005", item2.sku());
        assertEquals("Thịt thăn bò Úc", item2.name());
        assertEquals("khay", item2.unit());
        assertEquals(4, item2.quantityInStock());
        assertEquals(5, item2.minStock());

        verify(productRepository).findLowStock();
    }

    @Test
    void lowStockReturnsEmptyListWhenNoProductsLowOnStock() {
        when(productRepository.findLowStock()).thenReturn(List.of());

        List<LowStockItem> result = service.lowStock();

        assertTrue(result.isEmpty());
        verify(productRepository).findLowStock();
    }

    @Test
    void lowStockPreservesAscendingOrderOfStock() {
        Product outOfStock = new Product();
        outOfStock.setId(10L);
        outOfStock.setSku("TC-001");
        outOfStock.setName("Táo Envy");
        outOfStock.setUnit("kg");
        outOfStock.setQuantityInStock(0);
        outOfStock.setMinStock(20);

        Product criticalStock = new Product();
        criticalStock.setId(11L);
        criticalStock.setSku("TC-002");
        criticalStock.setName("Nho xanh Autumn Crisp");
        criticalStock.setUnit("hộp");
        criticalStock.setQuantityInStock(3);
        criticalStock.setMinStock(15);

        Product boundaryStock = new Product();
        boundaryStock.setId(12L);
        boundaryStock.setSku("TC-003");
        boundaryStock.setName("Cam Cara ruột đỏ");
        boundaryStock.setUnit("kg");
        boundaryStock.setQuantityInStock(10);
        boundaryStock.setMinStock(10);

        when(productRepository.findLowStock()).thenReturn(List.of(outOfStock, criticalStock, boundaryStock));

        List<LowStockItem> result = service.lowStock();

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).quantityInStock());
        assertEquals(3, result.get(1).quantityInStock());
        assertEquals(10, result.get(2).quantityInStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenStockEqualsMinStock() {
        Product product = new Product();
        product.setQuantityInStock(15);
        product.setMinStock(15);

        assertTrue(product.isLowStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenStockExceedsMinStockByOne() {
        Product product = new Product();
        product.setQuantityInStock(16);
        product.setMinStock(15);

        assertFalse(product.isLowStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenStockIsZero() {
        Product product = new Product();
        product.setQuantityInStock(0);
        product.setMinStock(10);

        assertTrue(product.isLowStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenMinStockIsZero() {
        Product product = new Product();
        product.setMinStock(0);

        product.setQuantityInStock(0);
        assertTrue(product.isLowStock());

        product.setQuantityInStock(1);
        assertFalse(product.isLowStock());
    }

    @Test
    void historyThrows404WhenProductDoesNotExist() {
        when(productRepository.existsById(999L)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> service.history(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy sản phẩm 999", exception.getMessage());
        verify(receiptDetailRepository, never()).findConfirmedByProductId(any(), any());
        verify(issueDetailRepository, never()).findConfirmedByProductId(any(), any());
    }

    @Test
    void historyReturnsCombinedAndSortedConfirmedReceiptsAndIssues() {
        when(productRepository.existsById(1L)).thenReturn(true);

        Instant t1 = Instant.parse("2026-11-20T08:00:00Z");
        Instant t2 = Instant.parse("2026-11-21T10:00:00Z");
        Instant t3 = Instant.parse("2026-11-22T14:00:00Z");

        StockReceipt r1 = new StockReceipt();
        r1.setCode("PN-20261120-001");
        r1.setConfirmedAt(t1);

        StockReceiptDetail d1 = new StockReceiptDetail();
        d1.setReceipt(r1);
        d1.setQuantity(50);

        StockReceipt r2 = new StockReceipt();
        r2.setCode("PN-20261121-002");
        r2.setConfirmedAt(t2);

        StockReceiptDetail d2 = new StockReceiptDetail();
        d2.setReceipt(r2);
        d2.setQuantity(30);

        StockIssue i1 = new StockIssue();
        i1.setCode("PX-20261122-001");
        i1.setConfirmedAt(t3);

        StockIssueDetail d3 = new StockIssueDetail();
        d3.setIssue(i1);
        d3.setQuantity(20);

        when(receiptDetailRepository.findConfirmedByProductId(eq(1L), any())).thenReturn(List.of(d1, d2));
        when(issueDetailRepository.findConfirmedByProductId(eq(1L), any())).thenReturn(List.of(d3));

        List<ProductHistoryItem> history = service.history(1L);

        assertEquals(3, history.size());

        assertEquals(t3, history.get(0).occurredAt());
        assertEquals(MovementDirection.OUT, history.get(0).direction());
        assertEquals(MovementDocumentType.ISSUE, history.get(0).documentType());
        assertEquals("PX-20261122-001", history.get(0).documentCode());
        assertEquals(20, history.get(0).quantity());

        assertEquals(t2, history.get(1).occurredAt());
        assertEquals(MovementDirection.IN, history.get(1).direction());
        assertEquals(MovementDocumentType.RECEIPT, history.get(1).documentType());
        assertEquals("PN-20261121-002", history.get(1).documentCode());
        assertEquals(30, history.get(1).quantity());

        assertEquals(t1, history.get(2).occurredAt());
        assertEquals(MovementDirection.IN, history.get(2).direction());
        assertEquals(MovementDocumentType.RECEIPT, history.get(2).documentType());
        assertEquals("PN-20261120-001", history.get(2).documentCode());
        assertEquals(50, history.get(2).quantity());
    }

    @Test
    void historyAllowsInactiveProduct() {
        when(productRepository.existsById(2L)).thenReturn(true);
        when(receiptDetailRepository.findConfirmedByProductId(eq(2L), any())).thenReturn(List.of());
        when(issueDetailRepository.findConfirmedByProductId(eq(2L), any())).thenReturn(List.of());

        List<ProductHistoryItem> history = service.history(2L);

        assertTrue(history.isEmpty());
        verify(productRepository).existsById(2L);
    }

    @Test
    void historyReturnsEmptyListWhenNoConfirmedDocuments() {
        when(productRepository.existsById(3L)).thenReturn(true);
        when(receiptDetailRepository.findConfirmedByProductId(eq(3L), any())).thenReturn(List.of());
        when(issueDetailRepository.findConfirmedByProductId(eq(3L), any())).thenReturn(List.of());

        List<ProductHistoryItem> history = service.history(3L);

        assertTrue(history.isEmpty());
    }

    @Test
    void historyOnlyContainsReceiptsWhenNoIssuesExist() {
        when(productRepository.existsById(4L)).thenReturn(true);

        Instant t1 = Instant.parse("2026-11-23T09:00:00Z");
        StockReceipt r1 = new StockReceipt();
        r1.setCode("PN-20261123-001");
        r1.setConfirmedAt(t1);

        StockReceiptDetail d1 = new StockReceiptDetail();
        d1.setReceipt(r1);
        d1.setQuantity(100);

        when(receiptDetailRepository.findConfirmedByProductId(eq(4L), any())).thenReturn(List.of(d1));
        when(issueDetailRepository.findConfirmedByProductId(eq(4L), any())).thenReturn(List.of());

        List<ProductHistoryItem> history = service.history(4L);

        assertEquals(1, history.size());
        assertEquals(MovementDirection.IN, history.get(0).direction());
        assertEquals(MovementDocumentType.RECEIPT, history.get(0).documentType());
        assertEquals(100, history.get(0).quantity());
    }

    @Test
    void historyOnlyContainsIssuesWhenNoReceiptsExist() {
        when(productRepository.existsById(5L)).thenReturn(true);

        Instant t1 = Instant.parse("2026-11-23T15:00:00Z");
        StockIssue i1 = new StockIssue();
        i1.setCode("PX-20261123-005");
        i1.setConfirmedAt(t1);

        StockIssueDetail d1 = new StockIssueDetail();
        d1.setIssue(i1);
        d1.setQuantity(15);

        when(receiptDetailRepository.findConfirmedByProductId(eq(5L), any())).thenReturn(List.of());
        when(issueDetailRepository.findConfirmedByProductId(eq(5L), any())).thenReturn(List.of(d1));

        List<ProductHistoryItem> history = service.history(5L);

        assertEquals(1, history.size());
        assertEquals(MovementDirection.OUT, history.get(0).direction());
        assertEquals(MovementDocumentType.ISSUE, history.get(0).documentType());
        assertEquals(15, history.get(0).quantity());
    }

    @Test
    void historyUsesConfirmedAtInsteadOfCreatedAt() {
        when(productRepository.existsById(10L)).thenReturn(true);

        Instant createdAt = Instant.parse("2026-11-20T08:00:00Z");
        Instant confirmedAt = Instant.parse("2026-11-20T14:30:00Z");

        StockReceipt receipt = new StockReceipt();
        receipt.setCode("PN-20261120-001");
        receipt.setCreatedAt(createdAt);
        receipt.setConfirmedAt(confirmedAt);

        StockReceiptDetail detail = new StockReceiptDetail();
        detail.setReceipt(receipt);
        detail.setQuantity(40);

        when(receiptDetailRepository.findConfirmedByProductId(eq(10L), any())).thenReturn(List.of(detail));
        when(issueDetailRepository.findConfirmedByProductId(eq(10L), any())).thenReturn(List.of());

        List<ProductHistoryItem> history = service.history(10L);

        assertEquals(1, history.size());
        assertEquals(confirmedAt, history.get(0).occurredAt());
    }

    @Test
    void historyHandlesDocumentsWithIdenticalTimestamp() {
        when(productRepository.existsById(11L)).thenReturn(true);

        Instant sameTime = Instant.parse("2026-11-21T09:00:00Z");

        StockReceipt receipt = new StockReceipt();
        receipt.setCode("PN-20261121-001");
        receipt.setConfirmedAt(sameTime);

        StockReceiptDetail receiptDetail = new StockReceiptDetail();
        receiptDetail.setReceipt(receipt);
        receiptDetail.setQuantity(50);

        StockIssue issue = new StockIssue();
        issue.setCode("PX-20261121-001");
        issue.setConfirmedAt(sameTime);

        StockIssueDetail issueDetail = new StockIssueDetail();
        issueDetail.setIssue(issue);
        issueDetail.setQuantity(20);

        when(receiptDetailRepository.findConfirmedByProductId(eq(11L), any())).thenReturn(List.of(receiptDetail));
        when(issueDetailRepository.findConfirmedByProductId(eq(11L), any())).thenReturn(List.of(issueDetail));

        List<ProductHistoryItem> history = service.history(11L);

        assertEquals(2, history.size());
        assertEquals(sameTime, history.get(0).occurredAt());
        assertEquals(sameTime, history.get(1).occurredAt());
    }

    @Test
    void historyPutsRowsWithoutConfirmedAtLastInsteadOfFailing() {
        when(productRepository.existsById(20L)).thenReturn(true);
        StockReceiptDetail undated = receiptDetail("PN-20261121-009", null, 5);
        StockReceiptDetail dated = receiptDetail("PN-20261121-001", Instant.parse("2026-11-21T09:00:00Z"), 7);
        when(receiptDetailRepository.findConfirmedByProductId(eq(20L), any())).thenReturn(List.of(undated, dated));
        when(issueDetailRepository.findConfirmedByProductId(eq(20L), any())).thenReturn(List.of());

        List<ProductHistoryItem> history = service.history(20L);

        assertEquals(List.of("PN-20261121-001", "PN-20261121-009"),
                history.stream().map(ProductHistoryItem::documentCode).toList());
    }

    @Test
    void historyReadsAtMostTheLatestRowsFromEachSideAndReturnsAtMostTheLimit() {
        when(productRepository.existsById(21L)).thenReturn(true);
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<StockReceiptDetail> manyReceipts = new ArrayList<>();
        for (int i = 0; i < InventoryServiceImpl.MAX_HISTORY_ITEMS; i++) {
            manyReceipts.add(receiptDetail("PN-%03d".formatted(i), base.plusSeconds(i), 1));
        }
        when(receiptDetailRepository.findConfirmedByProductId(eq(21L), any())).thenReturn(manyReceipts);
        when(issueDetailRepository.findConfirmedByProductId(eq(21L), any())).thenReturn(
                List.of(issueDetail("PX-NEWEST", base.plusSeconds(10_000), 2)));

        List<ProductHistoryItem> history = service.history(21L);

        assertEquals(InventoryServiceImpl.MAX_HISTORY_ITEMS, history.size());
        assertEquals("PX-NEWEST", history.get(0).documentCode());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(receiptDetailRepository).findConfirmedByProductId(eq(21L), page.capture());
        assertEquals(InventoryServiceImpl.MAX_HISTORY_ITEMS, page.getValue().getPageSize());
    }

    private static StockReceiptDetail receiptDetail(String code, Instant confirmedAt, int quantity) {
        StockReceipt receipt = new StockReceipt();
        receipt.setCode(code);
        receipt.setConfirmedAt(confirmedAt);
        StockReceiptDetail detail = new StockReceiptDetail();
        detail.setReceipt(receipt);
        detail.setQuantity(quantity);
        return detail;
    }

    private static StockIssueDetail issueDetail(String code, Instant confirmedAt, int quantity) {
        StockIssue issue = new StockIssue();
        issue.setCode(code);
        issue.setConfirmedAt(confirmedAt);
        StockIssueDetail detail = new StockIssueDetail();
        detail.setIssue(issue);
        detail.setQuantity(quantity);
        return detail;
    }
}

