package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.freshmart.smartstock.support.TestData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Receipt lifecycle (create -> confirm adds stock) and user management, end to end on a real MySQL. */
class ReceiptAndUserIntegrationTest extends AbstractIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private TestData data;
    private User admin;
    private User staff;
    private User otherStaff;

    @BeforeEach
    void setUp() {
        data = new TestData(userRepository, categoryRepository, productRepository, supplierRepository,
                receiptRepository, issueRepository);
        admin = loginUser(Role.ADMIN);
        staff = loginUser(Role.STAFF);
        otherStaff = loginUser(Role.STAFF);
    }

    @Test
    void confirmingReceiptAddsStockOnceAndSecondConfirmReturns409() throws Exception {
        Product product = data.product(10, 2);
        Supplier supplier = data.supplier();
        String token = token(staff);

        String created = mockMvc.perform(post("/api/receipts").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":%d,\"lines\":[{\"productId\":%d,\"quantity\":5,\"unitPrice\":9000}]}"
                                .formatted(supplier.getId(), product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.createdById").value(staff.getId()))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.data.id");

        mockMvc.perform(patch("/api/receipts/" + id + "/confirm").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedAt").isNotEmpty());
        assertEquals(15, productRepository.findById(product.getId()).orElseThrow().getQuantityInStock());

        mockMvc.perform(patch("/api/receipts/" + id + "/confirm").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
        assertEquals(15, productRepository.findById(product.getId()).orElseThrow().getQuantityInStock());
    }

    @Test
    void staffCannotConfirmAnotherStaffReceiptButAdminCan() throws Exception {
        Product product = data.product(0, 0);
        var receipt = data.receipt(staff, product, 4, com.freshmart.smartstock.domain.enums.DocumentStatus.DRAFT,
                null);

        mockMvc.perform(patch("/api/receipts/" + receipt.getId() + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, token(otherStaff)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/receipts/" + receipt.getId() + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andExpect(status().isOk());
        assertEquals(4, productRepository.findById(product.getId()).orElseThrow().getQuantityInStock());
    }

    @Test
    void userEndpointsAreAdminOnlyNeverExposePasswordHashAndNormalizeEmail() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, token(staff)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());

        String email = "New.User" + System.nanoTime() + "@Freshmart.test";
        String body = "{\"fullName\":\"New\",\"email\":\"%s\",\"password\":\"Secret@123\",\"role\":\"STAFF\"}"
                .formatted(email);
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace(email, email.toLowerCase())))
                .andExpect(status().isConflict());
    }

    private User loginUser(Role role) {
        User u = data.user(role);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return userRepository.save(u);
    }

    private String token(User user) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.data.accessToken");
    }
}
