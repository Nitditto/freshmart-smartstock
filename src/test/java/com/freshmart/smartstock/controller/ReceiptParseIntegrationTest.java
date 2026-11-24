package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.domain.entity.Product;
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

/** POST /api/receipts/parse end to end with the offline mock (tests never have an API key). */
class ReceiptParseIntegrationTest extends AbstractIntegrationTest {

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

    @BeforeEach
    void setUp() {
        data = new TestData(userRepository, categoryRepository, productRepository, supplierRepository,
                receiptRepository, issueRepository);
    }

    @Test
    void staffGetsSuggestionAndNothingIsSaved() throws Exception {
        Product tomato = data.product(0, 0);
        tomato.setName("Cà chua parse");
        productRepository.save(tomato);
        long receiptsBefore = receiptRepository.count();

        mockMvc.perform(post("/api/receipts/parse").header(HttpHeaders.AUTHORIZATION, token(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Cà chua parse 40kg x 26.000; Hàng lạ 3 gói\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mock").value(true))
                .andExpect(jsonPath("$.data.lines[0].productId").value(tomato.getId()))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(40))
                .andExpect(jsonPath("$.data.lines[0].unitPrice").value(26000))
                .andExpect(jsonPath("$.data.unmatched.length()").value(1));
        assertEquals(receiptsBefore, receiptRepository.count());
    }

    @Test
    void blankTextIs400AndAnonymousIs401() throws Exception {
        mockMvc.perform(post("/api/receipts/parse").header(HttpHeaders.AUTHORIZATION, token(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/receipts/parse").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Cà chua 1kg\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String token(Role role) throws Exception {
        User user = data.user(role);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        userRepository.save(user);
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.data.accessToken");
    }
}
