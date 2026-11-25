package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.domain.entity.Category;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueDetailRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptDetailRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.freshmart.smartstock.support.TestData;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

class CatalogIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Test@12345";

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
    private StockReceiptDetailRepository receiptDetailRepository;
    @Autowired
    private StockIssueDetailRepository issueDetailRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestData data;
    private User admin;
    private User staff;
    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        data = new TestData(userRepository, categoryRepository, productRepository, supplierRepository,
                receiptRepository, issueRepository);
        admin = loginUser(Role.ADMIN);
        staff = loginUser(Role.STAFF);
        adminToken = token(admin);
        staffToken = token(staff);
    }

    @Nested
    @DisplayName("Security and role based access control")
    class SecurityAndAccessControlTests {

        @Test
        @DisplayName("Staff cannot create product")
        void staffCannotCreateProduct() throws Exception {
            Category cat = data.category();
            String body = """
                    {
                      "sku": "STAFF-P1",
                      "name": "Staff product",
                      "categoryId": %d,
                      "unit": "kg",
                      "price": 50000,
                      "minStock": 10
                    }
                    """.formatted(cat.getId());

            mockMvc.perform(post("/api/products")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());

            assertFalse(productRepository.existsBySkuIgnoreCase("STAFF-P1"));
        }

        @Test
        @DisplayName("Staff cannot update product")
        void staffCannotUpdateProduct() throws Exception {
            Product p = data.product(10, 2);
            Category cat = data.category();
            String body = """
                    {
                      "sku": "%s",
                      "name": "Staff Attempt",
                      "categoryId": %d,
                      "unit": "kg",
                      "price": 60000,
                      "minStock": 10
                    }
                    """.formatted(p.getSku(), cat.getId());

            mockMvc.perform(put("/api/products/" + p.getId())
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Staff cannot hide product")
        void staffCannotHideProduct() throws Exception {
            Product p = data.product(10, 2);

            mockMvc.perform(delete("/api/products/" + p.getId())
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isForbidden());

            assertTrue(productRepository.findById(p.getId()).orElseThrow().isActive());
        }

        @Test
        @DisplayName("Staff cannot create supplier")
        void staffCannotCreateSupplier() throws Exception {
            String body = """
                    {
                      "name": "Staff Supplier",
                      "phone": "0987654321",
                      "address": "Ha Noi"
                    }
                    """;

            mockMvc.perform(post("/api/suppliers")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Staff cannot update supplier")
        void staffCannotUpdateSupplier() throws Exception {
            Supplier s = data.supplier();
            String body = """
                    {
                      "name": "Updated Supplier",
                      "phone": "0912345678",
                      "address": "Da Nang"
                    }
                    """;

            mockMvc.perform(put("/api/suppliers/" + s.getId())
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous user without token is rejected")
        void anonymousUserWithoutTokenIsRejected() throws Exception {
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/api/products/1"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Admin can create product and persists in database")
        void adminCanCreateProductAndPersistsInDatabase() throws Exception {
            Category cat = data.category();
            String sku = "ADM-PROD-" + System.nanoTime();
            String body = """
                    {
                      "sku": "%s",
                      "name": "Fresh Orange",
                      "categoryId": %d,
                      "unit": "kg",
                      "price": 45000,
                      "minStock": 15
                    }
                    """.formatted(sku, cat.getId());

            mockMvc.perform(post("/api/products")
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.sku").value(sku))
                    .andExpect(jsonPath("$.data.quantityInStock").value(0))
                    .andExpect(jsonPath("$.data.active").value(true));

            Product saved = productRepository.findAll().stream()
                    .filter(p -> sku.equalsIgnoreCase(p.getSku()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(saved);
            assertEquals("Fresh Orange", saved.getName());
            assertEquals(0, saved.getQuantityInStock());
            assertTrue(saved.isActive());
            assertEquals(15, saved.getMinStock());
        }

        @Test
        @DisplayName("Admin can create and update supplier")
        void adminCanCreateAndUpdateSupplier() throws Exception {
            String createBody = """
                    {
                      "name": "Fresh Farm Supplier",
                      "phone": "0901234567",
                      "address": "Lam Dong"
                    }
                    """;

            String response = mockMvc.perform(post("/api/suppliers")
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("Fresh Farm Supplier"))
                    .andReturn().getResponse().getContentAsString();

            Number idNum = JsonPath.read(response, "$.data.id");
            Long supplierId = idNum.longValue();

            Supplier fromDb = supplierRepository.findById(supplierId).orElseThrow();
            assertEquals("Fresh Farm Supplier", fromDb.getName());
            assertEquals("0901234567", fromDb.getPhone());
            assertEquals("Lam Dong", fromDb.getAddress());

            String updateBody = """
                    {
                      "name": "Fresh Farm High Tech",
                      "phone": "0909999999",
                      "address": "Duc Trong Lam Dong"
                    }
                    """;

            mockMvc.perform(put("/api/suppliers/" + supplierId)
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Fresh Farm High Tech"));

            Supplier updatedDb = supplierRepository.findById(supplierId).orElseThrow();
            assertEquals("Fresh Farm High Tech", updatedDb.getName());
            assertEquals("0909999999", updatedDb.getPhone());
        }
    }

    @Nested
    @DisplayName("Product business logic and database persistence")
    class BusinessLogicTests {

        @Test
        @DisplayName("Reject duplicate SKU ignoring case with 409 conflict")
        void createRejectsDuplicateSkuIgnoringCaseWith409() throws Exception {
            Category cat = data.category();
            String sku = "SKU-DUP-" + System.nanoTime();

            // First creation succeeds
            String body1 = """
                    {
                      "sku": "%s",
                      "name": "First item",
                      "categoryId": %d,
                      "unit": "kg",
                      "price": 20000,
                      "minStock": 5
                    }
                    """.formatted(sku.toUpperCase(), cat.getId());

            mockMvc.perform(post("/api/products")
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body1))
                    .andExpect(status().isCreated());

            // Second creation with lowercase duplicate SKU is rejected with 409 Conflict
            String body2 = """
                    {
                      "sku": "%s",
                      "name": "Duplicate item",
                      "categoryId": %d,
                      "unit": "kg",
                      "price": 30000,
                      "minStock": 5
                    }
                    """.formatted(sku.toLowerCase(), cat.getId());

            mockMvc.perform(post("/api/products")
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body2))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("Update product modifies fields and preserves existing stock")
        void updateModifiesFieldsAndPreservesExistingStock() throws Exception {
            Product p = data.product(88, 10);
            Category newCat = data.category();

            String updateBody = """
                    {
                      "sku": "%s",
                      "name": "Updated Name",
                      "categoryId": %d,
                      "unit": "thùng",
                      "price": 99000,
                      "minStock": 20
                    }
                    """.formatted(p.getSku(), newCat.getId());

            mockMvc.perform(put("/api/products/" + p.getId())
                            .header(HttpHeaders.AUTHORIZATION, adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated Name"))
                    .andExpect(jsonPath("$.data.unit").value("thùng"))
                    .andExpect(jsonPath("$.data.quantityInStock").value(88));

            // Verify fields are updated while existing stock remains unchanged
            Product fromDb = productRepository.findById(p.getId()).orElseThrow();
            assertEquals("Updated Name", fromDb.getName());
            assertEquals("thùng", fromDb.getUnit());
            assertEquals(88, fromDb.getQuantityInStock());
        }

        @Test
        @DisplayName("Soft delete hides product while remaining accessible by ID")
        void softDeleteLifecycleTest() throws Exception {
            Product p = data.product(25, 5);
            Long id = p.getId();

            // Soft-delete the product
            mockMvc.perform(delete("/api/products/" + id)
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk());

            // Verify active flag is set to false in database
            Product fromDb = productRepository.findById(id).orElseThrow();
            assertFalse(fromDb.isActive());

            // GET by ID still returns the inactive product
            mockMvc.perform(get("/api/products/" + id)
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.active").value(false));

            // Product is excluded from search results
            mockMvc.perform(get("/api/products?keyword=" + p.getSku())
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }

    @Nested
    @DisplayName("Page size and sorting whitelist boundaries")
    class BoundaryAnalysisTests {

        @Test
        @DisplayName("Page size accepts 100 and rejects 101")
        void pageSizeBoundaryTest() throws Exception {
            // Upper allowed boundary: size = 100
            mockMvc.perform(get("/api/products?page=0&size=100")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(100));

            // Exceeds allowed boundary: size = 101 -> 400 Bad Request
            mockMvc.perform(get("/api/products?page=0&size=101")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Sorting whitelist accepts valid fields and rejects unauthorized fields")
        void sortWhitelistBoundaryTest() throws Exception {
            // Whitelisted sort properties
            List<String> allowedFields = List.of("id", "sku", "name", "price", "quantityInStock");
            for (String field : allowedFields) {
                mockMvc.perform(get("/api/products?sort=" + field + ",asc")
                                .header(HttpHeaders.AUTHORIZATION, staffToken))
                        .andExpect(status().isOk());
            }

            // Non-whitelisted properties are rejected with 400 Bad Request
            mockMvc.perform(get("/api/products?sort=passwordHash,desc")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(get("/api/products?sort=category,asc")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Low stock rules and boundary checks")
    class LowStockTests {

        @Test
        @DisplayName("Low stock includes stock less than or equal to minimum and excludes hidden products")
        void lowStockBoundaryAndActiveFilterTest() throws Exception {
            // Stock equals minStock: 5 == 5 -> included
            Product pEqual = data.product(5, 5);
            // Stock exceeds minStock by 1: 6 > 5 -> excluded
            Product pAbove = data.product(6, 5);
            // Out of stock: 0 <= 5 -> included
            Product pZero = data.product(0, 5);
            // Low stock but inactive -> excluded
            Product pHidden = data.product(2, 5);
            pHidden.setActive(false);
            productRepository.save(pHidden);

            String response = mockMvc.perform(get("/api/inventory/low-stock")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            List<Long> lowStockIds = JsonPath.read(response, "$.data[*].productId");

            assertTrue(lowStockIds.contains(pEqual.getId()));
            assertTrue(lowStockIds.contains(pZero.getId()));
            assertFalse(lowStockIds.contains(pAbove.getId()));
            assertFalse(lowStockIds.contains(pHidden.getId()));
        }
    }

    @Nested
    @DisplayName("Product movement history from confirmed documents")
    class ProductHistoryTests {

        @Test
        @DisplayName("History returns confirmed receipts and issues sorted newest first")
        void historyReturnsOnlyConfirmedDocumentsSortedNewestFirst() throws Exception {
            Product p = data.product(100, 10);
            Instant t1 = Instant.parse("2026-11-20T08:00:00Z");
            Instant t2 = Instant.parse("2026-11-21T10:00:00Z");
            Instant t3 = Instant.parse("2026-11-22T14:00:00Z");

            // Receipt 1: CONFIRMED at t1 (qty = 50)
            data.receipt(admin, p, 50, DocumentStatus.CONFIRMED, t1);
            
            // Receipt 2: DRAFT (unconfirmed) -> ignored
            data.receipt(admin, p, 999, DocumentStatus.DRAFT, null);
            
            // Issue 1: CANCELLED -> ignored
            data.issue(staff, p, 888, IssueReason.SALE, DocumentStatus.CANCELLED, null);
            
            // Issue 2: CONFIRMED at t2 (qty = 20)
            data.issue(staff, p, 20, IssueReason.SALE, DocumentStatus.CONFIRMED, t2);
            
            // Receipt 3: CONFIRMED at t3 (qty = 30)
            data.receipt(admin, p, 30, DocumentStatus.CONFIRMED, t3);

            assertEquals(2, receiptDetailRepository.findConfirmedByProductId(p.getId()).size());
            assertEquals(1, issueDetailRepository.findConfirmedByProductId(p.getId()).size());

            mockMvc.perform(get("/api/products/" + p.getId() + "/history")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].occurredAt").value(t3.toString()))
                    .andExpect(jsonPath("$.data[0].direction").value("IN"))
                    .andExpect(jsonPath("$.data[0].documentType").value("RECEIPT"))
                    .andExpect(jsonPath("$.data[0].quantity").value(30))
                    .andExpect(jsonPath("$.data[1].occurredAt").value(t2.toString()))
                    .andExpect(jsonPath("$.data[1].direction").value("OUT"))
                    .andExpect(jsonPath("$.data[1].documentType").value("ISSUE"))
                    .andExpect(jsonPath("$.data[1].quantity").value(20))
                    .andExpect(jsonPath("$.data[2].occurredAt").value(t1.toString()))
                    .andExpect(jsonPath("$.data[2].direction").value("IN"))
                    .andExpect(jsonPath("$.data[2].documentType").value("RECEIPT"))
                    .andExpect(jsonPath("$.data[2].quantity").value(50));
        }

        @Test
        @DisplayName("History throws 404 when product does not exist")
        void historyThrows404WhenProductNotFound() throws Exception {
            mockMvc.perform(get("/api/products/999999/history")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    private User loginUser(Role role) {
        User u = data.user(role);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return userRepository.save(u);
    }

    private String token(User user) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.data.accessToken");
    }
}
