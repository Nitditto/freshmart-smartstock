package com.freshmart.smartstock.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.freshmart.smartstock.domain.entity.DocumentCounter;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.freshmart.smartstock.support.TestData;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Catches entity <-> Flyway drift. The app runs with ddl-auto=none, so a wrong column type or name would only
 * surface at runtime; here Hibernate validates every mapped table against the schema that V1..Vn really built.
 */
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class SchemaMappingTest extends AbstractIntegrationTest {

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
    private DocumentCounterRepository counterRepository;

    private TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(userRepository, categoryRepository, productRepository, supplierRepository,
                receiptRepository, issueRepository);
    }

    @Test
    void everyEntityRoundTripsThroughTheMigratedSchema() {
        User admin = data.user(Role.ADMIN);
        Product product = data.product(7, 3);
        Instant confirmedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        StockReceipt receipt = data.receipt(admin, product, 5, DocumentStatus.CONFIRMED, confirmedAt);
        StockIssue issue = data.issue(admin, product, 2, IssueReason.DAMAGED, DocumentStatus.DRAFT, null);
        DocumentCounter counter = counterRepository.save(new DocumentCounter("PN", LocalDate.of(2026, 11, 24)));

        StockReceipt loadedReceipt = receiptRepository.findById(receipt.getId()).orElseThrow();
        assertEquals(DocumentStatus.CONFIRMED, loadedReceipt.getStatus());
        assertEquals(confirmedAt, loadedReceipt.getConfirmedAt());
        assertEquals(1, loadedReceipt.getDetails().size());
        assertNotNull(loadedReceipt.getCreatedAt());

        StockIssue loadedIssue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueReason.DAMAGED, loadedIssue.getReason());
        assertEquals(Role.ADMIN, userRepository.findById(admin.getId()).orElseThrow().getRole());
        assertEquals(0, counterRepository.findById(counter.getId()).orElseThrow().getLastSeq());

        Product loadedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(7, loadedProduct.getQuantityInStock());
        assertNotNull(loadedProduct.getUpdatedAt());
    }
}
