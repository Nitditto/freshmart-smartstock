package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.domain.entity.Product;
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
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.freshmart.smartstock.support.TestData;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** stock-summary on a real MySQL: the GROUP BY queries, the CONFIRMED filter and the Asia/Ho_Chi_Minh day bounds. */
class StockSummaryIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Test@12345";
    private static final String PERIOD = "?from=2026-11-20&to=2026-11-22";
    /** 00:00 of 20/11 and of 23/11 in Asia/Ho_Chi_Minh (UTC+7). */
    private static final Instant PERIOD_START = Instant.parse("2026-11-19T17:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-11-22T17:00:00Z");

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

    @BeforeEach
    void setUp() {
        data = new TestData(userRepository, categoryRepository, productRepository, supplierRepository,
                receiptRepository, issueRepository);
        admin = loginUser(Role.ADMIN);
    }

    @Test
    void summaryAppliesOpeningPlusInMinusOutWithConfirmedDocumentsOnly() throws Exception {
        Product product = data.product(85, 0);
        Instant beforePeriod = PERIOD_START.minusSeconds(1);
        data.receipt(admin, product, 100, DocumentStatus.CONFIRMED, beforePeriod);
        data.issue(admin, product, 30, IssueReason.SALE, DocumentStatus.CONFIRMED, beforePeriod);
        data.receipt(admin, product, 20, DocumentStatus.CONFIRMED, PERIOD_START);
        data.issue(admin, product, 5, IssueReason.SALE, DocumentStatus.CONFIRMED, PERIOD_END.minusSeconds(1));
        data.receipt(admin, product, 7, DocumentStatus.CONFIRMED, PERIOD_END);
        data.receipt(admin, product, 50, DocumentStatus.DRAFT, null);
        data.issue(admin, product, 9, IssueReason.SALE, DocumentStatus.CANCELLED, null);

        String item = "$.data[?(@.productId == %d)]".formatted(product.getId());
        mockMvc.perform(get("/api/reports/stock-summary" + PERIOD).header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(item + ".openingQty").value(70))
                .andExpect(jsonPath(item + ".totalIn").value(20))
                .andExpect(jsonPath(item + ".totalOut").value(5))
                .andExpect(jsonPath(item + ".closingQty").value(85));
    }

    @Test
    void productWithoutDocumentsIsListedWithZeros() throws Exception {
        Product idle = data.product(0, 0);

        String item = "$.data[?(@.productId == %d)]".formatted(idle.getId());
        mockMvc.perform(get("/api/reports/stock-summary" + PERIOD).header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(item + ".sku").value(idle.getSku()))
                .andExpect(jsonPath(item + ".closingQty").value(0));
    }

    @Test
    void fromAfterToIs400() throws Exception {
        mockMvc.perform(get("/api/reports/stock-summary?from=2026-11-23&to=2026-11-20")
                        .header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffIsForbiddenAndAnonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reports/stock-summary" + PERIOD)
                        .header(HttpHeaders.AUTHORIZATION, token(loginUser(Role.STAFF))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/stock-summary" + PERIOD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDownloadsCsvWithBomAndAttachmentName() throws Exception {
        Product product = data.product(0, 0);
        data.receipt(admin, product, 12, DocumentStatus.CONFIRMED, PERIOD_START.plusSeconds(60));

        byte[] csv = mockMvc.perform(get("/api/reports/stock-summary/export" + PERIOD)
                        .header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stock-summary_2026-11-20_2026-11-22.csv\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertEquals((byte) 0xEF, csv[0]);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("Mã SP,SKU,Tên sản phẩm,Tồn đầu,Nhập,Xuất,Tồn cuối\r\n"));
        assertTrue(text.contains("%d,%s,%s,0,12,0,12\r\n".formatted(product.getId(), product.getSku(),
                product.getName())));
    }

    @Test
    void exportIsAdminOnlyAndValidatesThePeriodAsJson() throws Exception {
        mockMvc.perform(get("/api/reports/stock-summary/export" + PERIOD)
                        .header(HttpHeaders.AUTHORIZATION, token(loginUser(Role.STAFF))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/stock-summary/export?from=2026-11-23&to=2026-11-20")
                        .header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
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
