package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.entity.StockIssueDetail;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.dto.inventory.ShortageItem;
import com.freshmart.smartstock.dto.issue.IssueConfirmResponse;
import com.freshmart.smartstock.dto.issue.IssueLineRequest;
import com.freshmart.smartstock.dto.issue.IssueRequest;
import com.freshmart.smartstock.dto.issue.IssueResponse;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.service.impl.IssueServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class IssueServiceImplTest {

    @Mock
    private StockIssueRepository issueRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentCodeGenerator codeGenerator;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-11-21T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @InjectMocks
    private IssueServiceImpl service;

    private User staffUser;
    private User adminUser;
    private Product activeProduct1;
    private Product activeProduct2;

    @BeforeEach
    void setUp() {
        staffUser = new User();
        staffUser.setId(10L);
        staffUser.setEmail("staff@freshmart.local");
        staffUser.setFullName("Staff Member");
        staffUser.setRole(Role.STAFF);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@freshmart.local");
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);

        activeProduct1 = new Product();
        activeProduct1.setId(101L);
        activeProduct1.setSku("SP-001");
        activeProduct1.setName("Táo Envy");
        activeProduct1.setUnit("kg");
        activeProduct1.setPrice(BigDecimal.valueOf(80000));
        activeProduct1.setQuantityInStock(50);
        activeProduct1.setMinStock(10);
        activeProduct1.setActive(true);

        activeProduct2 = new Product();
        activeProduct2.setId(102L);
        activeProduct2.setSku("SP-002");
        activeProduct2.setName("Cam Sành");
        activeProduct2.setUnit("kg");
        activeProduct2.setPrice(BigDecimal.valueOf(45000));
        activeProduct2.setQuantityInStock(20);
        activeProduct2.setMinStock(5);
        activeProduct2.setActive(true);

        setAuthUser(10L, "staff@freshmart.local", Role.STAFF);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthUser(Long id, String email, Role role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        AuthUser principal = new AuthUser(id, email, role);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    // ==================== CREATE ====================

    @Test
    void createRejectsDuplicateProductIdsWith400() {
        IssueRequest request = new IssueRequest(
                IssueReason.SALE, "Xuất bán",
                List.of(new IssueLineRequest(101L, 5), new IssueLineRequest(101L, 3)));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("bị lặp"));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void createRejectsNonExistentProductWith404() {
        IssueRequest request = new IssueRequest(
                IssueReason.SALE, "Xuất bán",
                List.of(new IssueLineRequest(999L, 5)));
        when(productRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getMessage().contains("Không tìm thấy"));
    }

    @Test
    void createRejectsHiddenProductWith400() {
        Product hiddenProduct = new Product();
        hiddenProduct.setId(103L);
        hiddenProduct.setName("Sữa cũ");
        hiddenProduct.setActive(false);

        when(productRepository.findAllById(anyList())).thenReturn(List.of(hiddenProduct));

        IssueRequest request = new IssueRequest(
                IssueReason.SALE, "Xuất bán",
                List.of(new IssueLineRequest(103L, 5)));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("đã bị ẩn"));
    }

    @Test
    void createSavesDraftWithCodeAndCreatorFromToken() {
        IssueRequest request = new IssueRequest(
                IssueReason.SALE, "  Xuất bán lẻ  ",
                List.of(new IssueLineRequest(101L, 5)));

        when(productRepository.findAllById(anyList())).thenReturn(List.of(activeProduct1));
        when(codeGenerator.next("PX")).thenReturn("PX-20261121-001");
        when(userRepository.getReferenceById(10L)).thenReturn(staffUser);

        when(issueRepository.save(any(StockIssue.class))).thenAnswer(inv -> {
            StockIssue issue = inv.getArgument(0);
            issue.setId(1L);
            return issue;
        });

        StockIssue savedIssue = new StockIssue();
        savedIssue.setId(1L);
        savedIssue.setCode("PX-20261121-001");
        savedIssue.setReason(IssueReason.SALE);
        savedIssue.setStatus(DocumentStatus.DRAFT);
        savedIssue.setNote("Xuất bán lẻ");
        savedIssue.setCreatedBy(staffUser);
        savedIssue.setCreatedAt(Instant.now());
        StockIssueDetail detail = new StockIssueDetail();
        detail.setId(1001L);
        detail.setProduct(activeProduct1);
        detail.setQuantity(5);
        savedIssue.addDetail(detail);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(savedIssue));

        IssueResponse response = service.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("PX-20261121-001", response.code());
        assertEquals(DocumentStatus.DRAFT, response.status());
        assertEquals(10L, response.createdById());
        assertEquals("Xuất bán lẻ", response.note());
        assertEquals(1, response.lines().size());
    }

    // ==================== SEARCH & GET ====================

    @Test
    void searchRejectsFromAfterToWith400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.search(
                DocumentStatus.DRAFT, LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 20), PageRequest.of(0, 10)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void searchReturnsPageResponse() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        issue.setReason(IssueReason.SALE);
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(staffUser);
        issue.setCreatedAt(Instant.now());

        Page<StockIssue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.search(any(), any(), any(), any())).thenReturn(page);

        PageResponse<IssueResponse> result = service.search(null, null, null, PageRequest.of(0, 20));
        assertEquals(1, result.items().size());
        assertEquals("PX-20261121-001", result.items().get(0).code());
    }

    @Test
    void getThrows404WhenNotFound() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.get(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ==================== UPDATE ====================

    @Test
    void updateRejectsOtherStaffWith403() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        User otherStaff = new User();
        otherStaff.setId(99L);
        issue.setCreatedBy(otherStaff);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));

        IssueRequest request = new IssueRequest(IssueReason.SALE, "Note", List.of(new IssueLineRequest(101L, 2)));

        ApiException ex = assertThrows(ApiException.class, () -> service.update(1L, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void updateRejectsNonDraftWith409() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        issue.setStatus(DocumentStatus.CONFIRMED);
        issue.setCreatedBy(staffUser);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));

        IssueRequest request = new IssueRequest(IssueReason.SALE, "Note", List.of(new IssueLineRequest(101L, 2)));

        ApiException ex = assertThrows(ApiException.class, () -> service.update(1L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ==================== CONFIRM (CORE BUSINESS) ====================

    @Test
    void confirmRejectsNonOwnerStaffWith403() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        User otherStaff = new User();
        otherStaff.setId(99L);
        issue.setCreatedBy(otherStaff);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void confirmRejectsDamagedReasonByStaffWith403() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        issue.setReason(IssueReason.DAMAGED);
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(staffUser);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Chỉ ADMIN"));
    }

    @Test
    void confirmRejectsAlreadyConfirmedWith409() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        issue.setReason(IssueReason.SALE);
        issue.setStatus(DocumentStatus.CONFIRMED);
        issue.setCreatedBy(staffUser);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void confirmThrows409AndDoesNotDeductWhenAnyProductHasInsufficientStock() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        issue.setReason(IssueReason.SALE);
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(staffUser);

        StockIssueDetail detail1 = new StockIssueDetail();
        detail1.setProduct(activeProduct1);
        detail1.setQuantity(10); // available: 50 -> OK
        issue.addDetail(detail1);

        StockIssueDetail detail2 = new StockIssueDetail();
        detail2.setProduct(activeProduct2);
        detail2.setQuantity(30); // available: 20 -> INSUFFICIENT!
        issue.addDetail(detail2);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));
        when(productRepository.findAllByIdForUpdate(anyList())).thenReturn(List.of(activeProduct1, activeProduct2));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getData() instanceof List<?>);
        List<?> data = (List<?>) ex.getData();
        assertEquals(1, data.size());
        ShortageItem shortage = (ShortageItem) data.get(0);
        assertEquals(102L, shortage.productId());
        assertEquals(30, shortage.requested());
        assertEquals(20, shortage.available());

        // Assert that NO stock was deducted!
        assertEquals(50, activeProduct1.getQuantityInStock());
        assertEquals(20, activeProduct2.getQuantityInStock());
        assertEquals(DocumentStatus.DRAFT, issue.getStatus());
    }

    @Test
    void confirmDeductsStockAndDetectsNewlyLowStockWhenSufficient() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        issue.setReason(IssueReason.SALE);
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(staffUser);
        issue.setCreatedAt(Instant.now());

        // activeProduct1: stock=50, minStock=10, deduct 42 -> remaining 8 (<= minStock -> LOW STOCK!)
        StockIssueDetail detail1 = new StockIssueDetail();
        detail1.setProduct(activeProduct1);
        detail1.setQuantity(42);
        issue.addDetail(detail1);

        // activeProduct2: stock=20, minStock=5, deduct 10 -> remaining 10 (> minStock -> NOT LOW STOCK)
        StockIssueDetail detail2 = new StockIssueDetail();
        detail2.setProduct(activeProduct2);
        detail2.setQuantity(10);
        issue.addDetail(detail2);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));
        when(productRepository.findAllByIdForUpdate(anyList())).thenReturn(List.of(activeProduct1, activeProduct2));
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        IssueConfirmResponse response = service.confirm(1L);

        assertEquals(DocumentStatus.CONFIRMED, issue.getStatus());
        assertNotNull(issue.getConfirmedAt());
        assertEquals(8, activeProduct1.getQuantityInStock());
        assertEquals(10, activeProduct2.getQuantityInStock());

        // Check newly low stock items
        assertEquals(1, response.newlyLowStock().size());
        assertEquals(101L, response.newlyLowStock().get(0).productId());
        assertEquals(8, response.newlyLowStock().get(0).quantityInStock());
    }

    @Test
    void confirmAllowsAdminToConfirmDamagedIssue() {
        setAuthUser(1L, "admin@freshmart.local", Role.ADMIN);

        StockIssue issue = new StockIssue();
        issue.setId(2L);
        issue.setCode("PX-20261121-002");
        issue.setReason(IssueReason.DAMAGED);
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(staffUser); // created by staff, admin confirms

        StockIssueDetail detail = new StockIssueDetail();
        detail.setProduct(activeProduct1);
        detail.setQuantity(5);
        issue.addDetail(detail);

        when(issueRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(issue));
        when(productRepository.findAllByIdForUpdate(anyList())).thenReturn(List.of(activeProduct1));
        when(issueRepository.findById(2L)).thenReturn(Optional.of(issue));

        IssueConfirmResponse response = service.confirm(2L);

        assertEquals(DocumentStatus.CONFIRMED, issue.getStatus());
        assertEquals(45, activeProduct1.getQuantityInStock());
    }

    // ==================== CANCEL ====================

    @Test
    void cancelRejectsNonOwnerWith403() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        User otherStaff = new User();
        otherStaff.setId(99L);
        issue.setCreatedBy(otherStaff);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));

        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void cancelChangesStatusToCancelled() {
        StockIssue issue = new StockIssue();
        issue.setId(1L);
        issue.setCode("PX-20261121-001");
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(staffUser);

        when(issueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        IssueResponse response = service.cancel(1L);

        assertEquals(DocumentStatus.CANCELLED, issue.getStatus());
        verify(issueRepository).save(issue);
    }

    // ==================== GAPS FROM THE PLAN (member-c §5) ====================

    @Test
    void searchRejectsSortOutsideTheWhitelistWith400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.search(
                null, null, null, PageRequest.of(0, 10, Sort.by("passwordHash"))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(issueRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    void searchRejectsPageSizeAboveTheLimitWith400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.search(null, null, null, PageRequest.of(0, 1000)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void updateConfirmAndCancelReturn404ForUnknownIssue() {
        when(issueRepository.findByIdForUpdate(77L)).thenReturn(Optional.empty());
        IssueRequest request = new IssueRequest(IssueReason.SALE, null,
                List.of(new IssueLineRequest(activeProduct1.getId(), 1)));

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class, () -> service.update(77L, request)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.confirm(77L)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.cancel(77L)).getStatus());
    }

    @Test
    void adminCanUpdateAnotherStaffsDraftAndLinesAreReplaced() {
        setAuthUser(1L, "admin@freshmart.local", Role.ADMIN);
        StockIssue issue = draftIssue(activeProduct1, 3);
        when(issueRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(issue));
        when(productRepository.findAllById(any())).thenReturn(List.of(activeProduct2));
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));

        IssueResponse response = service.update(5L, new IssueRequest(IssueReason.INTERNAL, "  ghi chú  ",
                List.of(new IssueLineRequest(activeProduct2.getId(), 4))));

        assertEquals(IssueReason.INTERNAL, response.reason());
        assertEquals("ghi chú", issue.getNote());
        assertEquals(1, issue.getDetails().size());
        assertEquals(activeProduct2, issue.getDetails().get(0).getProduct());
        verify(issueRepository).saveAndFlush(issue);
    }

    @Test
    void stockLandingExactlyOnMinStockCountsAsNewlyLow() {
        StockIssue issue = draftIssue(activeProduct2, 15);
        when(issueRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(issue));
        when(productRepository.findAllByIdForUpdate(anyList())).thenReturn(List.of(activeProduct2));
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));

        IssueConfirmResponse response = service.confirm(5L);

        assertEquals(5, activeProduct2.getQuantityInStock());
        assertEquals(List.of(102L), response.newlyLowStock().stream().map(i -> i.productId()).toList());
    }

    private StockIssue draftIssue(Product product, int quantity) {
        StockIssue issue = new StockIssue();
        issue.setId(5L);
        issue.setCode("PX-20261121-005");
        issue.setReason(IssueReason.SALE);
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(staffUser);
        issue.setCreatedAt(clock.instant());
        StockIssueDetail detail = new StockIssueDetail();
        detail.setProduct(product);
        detail.setQuantity(quantity);
        issue.addDetail(detail);
        return issue;
    }
}
