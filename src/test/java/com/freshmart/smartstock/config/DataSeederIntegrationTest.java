package com.freshmart.smartstock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** Runs the real startup seed on an empty MySQL, exactly like `docker compose down -v && up` before the demo. */
@TestPropertySource(properties = {"app.seed.enabled=true", "app.seed.default-password=Seed@12345"})
class DataSeederIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSeeder dataSeeder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private StockReceiptRepository receiptRepository;
    @Autowired
    private StockIssueRepository issueRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void seedMatchesSpecCountsAndRunningItAgainChangesNothing() {
        assertSpecCounts();
        Map<Long, Integer> stockBefore = stockById();

        dataSeeder.run();

        assertSpecCounts();
        assertEquals(stockBefore, stockById());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void documentsCoverEveryStatusAndDamagedIssueWaitsForAdmin() {
        List<StockReceipt> receipts = receiptRepository.findAll();
        List<StockIssue> issues = issueRepository.findAll();

        assertEquals(5, receipts.stream().filter(r -> r.getStatus() == DocumentStatus.CONFIRMED).count());
        assertEquals(1, receipts.stream().filter(r -> r.getStatus() == DocumentStatus.DRAFT).count());
        assertEquals(1, receipts.stream().filter(r -> r.getStatus() == DocumentStatus.CANCELLED).count());
        assertEquals(2, issues.stream().filter(i -> i.getStatus() == DocumentStatus.CONFIRMED).count());
        StockIssue damaged = issues.stream().filter(i -> i.getReason() == IssueReason.DAMAGED).findFirst()
                .orElseThrow();
        assertEquals(DocumentStatus.DRAFT, damaged.getStatus());
    }

    @Test
    void everyUnitOfStockIsExplainedByConfirmedDocuments() {
        Map<Long, Integer> expected = transactionTemplate.execute(status -> confirmedMovements());

        productRepository.findAll().forEach(p ->
                assertEquals(expected.getOrDefault(p.getId(), 0), p.getQuantityInStock(), p.getSku()));
        assertEquals(List.of("DUO-006", "TCA-004", "RCQ-001", "RCQ-003"),
                productRepository.findLowStock().stream().map(Product::getSku).toList());
    }

    private void assertSpecCounts() {
        assertEquals(1, userRepository.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).count());
        assertEquals(2, userRepository.findAll().stream().filter(u -> u.getRole() == Role.STAFF).count());
        assertEquals(5, categoryRepository.count());
        assertEquals(30, productRepository.count());
        assertEquals(3, supplierRepository.count());
        assertEquals(10, receiptRepository.count() + issueRepository.count());
    }

    private Map<Long, Integer> stockById() {
        Map<Long, Integer> stock = new HashMap<>();
        productRepository.findAll().forEach(p -> stock.put(p.getId(), p.getQuantityInStock()));
        return stock;
    }

    private Map<Long, Integer> confirmedMovements() {
        Map<Long, Integer> net = new HashMap<>();
        receiptRepository.findAll().stream().filter(r -> r.getStatus() == DocumentStatus.CONFIRMED)
                .flatMap(r -> r.getDetails().stream())
                .forEach(d -> net.merge(d.getProduct().getId(), d.getQuantity(), Integer::sum));
        issueRepository.findAll().stream().filter(i -> i.getStatus() == DocumentStatus.CONFIRMED)
                .flatMap(i -> i.getDetails().stream())
                .forEach(d -> net.merge(d.getProduct().getId(), -d.getQuantity(), Integer::sum));
        return net;
    }
}
