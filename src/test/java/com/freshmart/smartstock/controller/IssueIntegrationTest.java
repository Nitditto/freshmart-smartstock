package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.security.JwtService;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.freshmart.smartstock.support.TestData;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

class IssueIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private StockReceiptRepository stockReceiptRepository;

    @Autowired
    private StockIssueRepository stockIssueRepository;

    @Autowired
    private JwtService jwtService;

    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData(
                userRepository, categoryRepository, productRepository,
                supplierRepository, stockReceiptRepository, stockIssueRepository);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(new AuthUser(user.getId(), user.getEmail(), user.getRole()));
    }

    @Test
    void confirmDeductsStockInDatabase() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(50, 10);
        StockIssue issue = testData.issue(staff, product, 15, IssueReason.SALE, DocumentStatus.DRAFT, null);

        mockMvc.perform(patch("/api/issues/{id}/confirm", issue.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.issue.status").value("CONFIRMED"));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(35, updated.getQuantityInStock());
    }

    @Test
    void confirmFailsWith409AndDoesNotDeductWhenStockInsufficient() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(10, 2);
        StockIssue issue = testData.issue(staff, product, 25, IssueReason.SALE, DocumentStatus.DRAFT, null);

        mockMvc.perform(patch("/api/issues/{id}/confirm", issue.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(staff)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.data[0].requested").value(25))
                .andExpect(jsonPath("$.data[0].available").value(10));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(10, updated.getQuantityInStock());
    }

    @Test
    void confirmDamagedRequiresAdmin() throws Exception {
        User staff = testData.user(Role.STAFF);
        User admin = testData.user(Role.ADMIN);
        Product product = testData.product(20, 5);
        StockIssue issue = testData.issue(staff, product, 5, IssueReason.DAMAGED, DocumentStatus.DRAFT, null);

        // Staff confirm damaged -> 403 Forbidden
        mockMvc.perform(patch("/api/issues/{id}/confirm", issue.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(staff)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Admin confirm damaged -> 200 OK
        mockMvc.perform(patch("/api/issues/{id}/confirm", issue.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(15, updated.getQuantityInStock());
    }

    @Test
    void concurrentConfirmNeverOversells() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(10, 2); // Initial stock = 10
        StockIssue issue1 = testData.issue(staff, product, 8, IssueReason.SALE, DocumentStatus.DRAFT, null);
        StockIssue issue2 = testData.issue(staff, product, 8, IssueReason.SALE, DocumentStatus.DRAFT, null);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        String token = bearer(staff);

        Runnable task1 = () -> {
            try {
                ready.countDown();
                go.await();
                MvcResult result = mockMvc.perform(patch("/api/issues/{id}/confirm", issue1.getId())
                                .header(HttpHeaders.AUTHORIZATION, token))
                        .andReturn();
                int httpStatus = result.getResponse().getStatus();
                if (httpStatus == 200) successCount.incrementAndGet();
                else if (httpStatus == 409) conflictCount.incrementAndGet();
            } catch (Exception ignored) {
            }
        };

        Runnable task2 = () -> {
            try {
                ready.countDown();
                go.await();
                MvcResult result = mockMvc.perform(patch("/api/issues/{id}/confirm", issue2.getId())
                                .header(HttpHeaders.AUTHORIZATION, token))
                        .andReturn();
                int httpStatus = result.getResponse().getStatus();
                if (httpStatus == 200) successCount.incrementAndGet();
                else if (httpStatus == 409) conflictCount.incrementAndGet();
            } catch (Exception ignored) {
            }
        };

        pool.submit(task1);
        pool.submit(task2);

        ready.await();
        go.countDown();
        pool.shutdown();

        // Exactly one should succeed (200), and one should fail (409)
        assertEquals(1, successCount.get());
        assertEquals(1, conflictCount.get());

        // Final stock must be 10 - 8 = 2, NEVER negative
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(2, updated.getQuantityInStock());
    }
}
