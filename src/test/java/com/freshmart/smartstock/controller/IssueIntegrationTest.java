package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class IssueIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCURRENCY_TIMEOUT_SECONDS = 30;

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
    void concurrentConfirmOfTwoIssuesNeverOversells() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(10, 2);
        StockIssue first = testData.issue(staff, product, 8, IssueReason.SALE, DocumentStatus.DRAFT, null);
        StockIssue second = testData.issue(staff, product, 8, IssueReason.SALE, DocumentStatus.DRAFT, null);

        List<Integer> statuses = confirmConcurrently(bearer(staff), first.getId(), second.getId());

        assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
        assertEquals(2, productRepository.findById(product.getId()).orElseThrow().getQuantityInStock());
    }

    @Test
    void concurrentConfirmOfTheSameIssueDeductsOnce() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(10, 2);
        StockIssue issue = testData.issue(staff, product, 3, IssueReason.SALE, DocumentStatus.DRAFT, null);

        List<Integer> statuses = confirmConcurrently(bearer(staff), issue.getId(), issue.getId());

        assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
        assertEquals(7, productRepository.findById(product.getId()).orElseThrow().getQuantityInStock());
    }

    @Test
    void updateKeepingTheSameProductDoesNotBreakTheUniqueLineConstraint() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(10, 2);
        StockIssue issue = testData.issue(staff, product, 3, IssueReason.SALE, DocumentStatus.DRAFT, null);

        mockMvc.perform(put("/api/issues/{id}", issue.getId()).header(HttpHeaders.AUTHORIZATION, bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"INTERNAL\",\"lines\":[{\"productId\":%d,\"quantity\":5}]}"
                                .formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reason").value("INTERNAL"))
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(5));
    }

    @Test
    void cancelTwiceIs409AndCancelledIssueCannotBeConfirmed() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(10, 2);
        StockIssue issue = testData.issue(staff, product, 3, IssueReason.SALE, DocumentStatus.DRAFT, null);
        String token = bearer(staff);

        mockMvc.perform(patch("/api/issues/{id}/cancel", issue.getId()).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mockMvc.perform(patch("/api/issues/{id}/cancel", issue.getId()).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/issues/{id}/confirm", issue.getId()).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
        assertEquals(10, productRepository.findById(product.getId()).orElseThrow().getQuantityInStock());
    }

    @Test
    void listFiltersByStatusAndRejectsUnknownSort() throws Exception {
        User staff = testData.user(Role.STAFF);
        Product product = testData.product(10, 2);
        StockIssue cancelled = testData.issue(staff, product, 1, IssueReason.SALE, DocumentStatus.CANCELLED, null);
        String token = bearer(staff);

        mockMvc.perform(get("/api/issues").param("status", "CANCELLED").param("sort", "id,desc")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(cancelled.getId()))
                .andExpect(jsonPath("$.data.items[0].createdByName").value(staff.getFullName()));
        mockMvc.perform(get("/api/issues").param("sort", "passwordHash").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    /** Starts both confirms at the same instant and waits for both answers (no sleep, no swallowed errors). */
    private List<Integer> confirmConcurrently(String token, Long firstId, Long secondId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Long id : List.of(firstId, secondId)) {
                results.add(pool.submit(() -> {
                    go.await();
                    return mockMvc.perform(patch("/api/issues/{id}/confirm", id)
                            .header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getStatus();
                }));
            }
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }
}
