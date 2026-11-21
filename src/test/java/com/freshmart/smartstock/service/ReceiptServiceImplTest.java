package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.dto.receipt.ReceiptLineRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptResponse;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.service.impl.ReceiptServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    private static final Long OWNER_ID = 10L;
    private static final Long OTHER_ID = 20L;
    private static final Long RECEIPT_ID = 100L;
    private static final Long SUPPLIER_ID = 5L;
    private static final Instant NOW = Instant.parse("2026-11-23T03:00:00Z");

    @Mock
    private StockReceiptRepository receiptRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentCodeGenerator codeGenerator;

    private ReceiptServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new ReceiptServiceImpl(receiptRepository, supplierRepository, productRepository, userRepository,
                codeGenerator, clock);
        authenticateAs(OWNER_ID, Role.STAFF);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- create ----------

    @Test
    void createSetsCreatorFromTokenAndGeneratedCode() {
        Product product = product(1L, 3);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier()));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));
        when(codeGenerator.next(DocumentCodeGenerator.RECEIPT_PREFIX)).thenReturn("PN-20261123-001");
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(user(OWNER_ID));
        when(receiptRepository.save(any(StockReceipt.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse response = service.create(request(line(1L, 4)));

        assertEquals("PN-20261123-001", response.code());
        assertEquals(OWNER_ID, response.createdById());
        assertEquals(DocumentStatus.DRAFT, response.status());
        assertEquals(1, response.lines().size());
        assertEquals(4, response.lines().get(0).quantity());
    }

    @Test
    void createRejectsUnknownSupplierWith404() {
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request(line(1L, 1))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateProductLinesWith400() {
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.create(request(line(1L, 1), line(1L, 2))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createRejectsHiddenProductWith400() {
        Product hidden = product(1L, 0);
        hidden.setActive(false);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier()));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(hidden));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request(line(1L, 1))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createRejectsUnknownProductWith404() {
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier()));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request(line(1L, 1))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ---------- get / search ----------

    @Test
    void getReturns404WhenMissing() {
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.get(RECEIPT_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getReturnsReceiptWithLines() {
        when(receiptRepository.findById(RECEIPT_ID))
                .thenReturn(Optional.of(draftReceipt(OWNER_ID, product(1L, 0), 2)));

        assertEquals(1, service.get(RECEIPT_ID).lines().size());
    }

    @Test
    void searchRejectsFromAfterToWith400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.search(null,
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 1), PageRequest.of(0, 10)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void searchRejectsNonWhitelistedSortWith400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.search(null, null, null,
                PageRequest.of(0, 10, Sort.by("supplier"))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void searchRejectsOversizedPageWith400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.search(null, null, null,
                PageRequest.of(0, 101)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void searchPassesInclusiveStartAndExclusiveNextDayBounds() {
        PageRequest page = PageRequest.of(0, 10, Sort.by("id"));
        Instant from = LocalDate.of(2026, 11, 20).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant to = LocalDate.of(2026, 11, 22).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        when(receiptRepository.search(DocumentStatus.DRAFT, from, to, page))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(draftReceipt(OWNER_ID, product(1L, 0), 1)), page, 1));

        var result = service.search(DocumentStatus.DRAFT, LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 21), page);

        assertEquals(1, result.totalElements());
    }

    // ---------- update ----------

    @Test
    void updateByOtherStaffIsForbiddenWith403() {
        authenticateAs(OTHER_ID, Role.STAFF);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID))
                .thenReturn(Optional.of(draftReceipt(OWNER_ID, product(1L, 0), 1)));

        ApiException ex = assertThrows(ApiException.class, () -> service.update(RECEIPT_ID, request(line(1L, 1))));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void updateByAdminReplacesLinesAndSupplier() {
        authenticateAs(OTHER_ID, Role.ADMIN);
        Product kept = product(1L, 0);
        Product added = product(2L, 0);
        StockReceipt receipt = draftReceipt(OWNER_ID, kept, 1);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier()));
        when(productRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(kept, added));
        when(receiptRepository.save(receipt)).thenReturn(receipt);

        ReceiptResponse response = service.update(RECEIPT_ID, request(line(1L, 7), line(2L, 3)));

        assertEquals(2, response.lines().size());
        assertEquals(7, response.lines().get(0).quantity());
        verify(receiptRepository).saveAndFlush(receipt);
    }

    @Test
    void updateConfirmedReceiptReturns409() {
        StockReceipt receipt = draftReceipt(OWNER_ID, product(1L, 0), 1);
        receipt.setStatus(DocumentStatus.CONFIRMED);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        ApiException ex = assertThrows(ApiException.class, () -> service.update(RECEIPT_ID, request(line(1L, 1))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void updateReturns404WhenReceiptMissing() {
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.update(RECEIPT_ID, request(line(1L, 1))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ---------- cancel ----------

    @Test
    void cancelMovesDraftToCancelled() {
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID))
                .thenReturn(Optional.of(draftReceipt(OWNER_ID, product(1L, 0), 1)));

        assertEquals(DocumentStatus.CANCELLED, service.cancel(RECEIPT_ID).status());
    }

    @Test
    void cancelCancelledReceiptReturns409() {
        StockReceipt receipt = draftReceipt(OWNER_ID, product(1L, 0), 1);
        receipt.setStatus(DocumentStatus.CANCELLED);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(RECEIPT_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void cancelByOtherStaffReturns403() {
        authenticateAs(OTHER_ID, Role.STAFF);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID))
                .thenReturn(Optional.of(draftReceipt(OWNER_ID, product(1L, 0), 1)));

        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(RECEIPT_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ---------- confirm ----------

    @Test
    void confirmIncreasesStockForEveryLineAndSetsConfirmedAt() {
        Product a = product(1L, 10);
        Product b = product(2L, 0);
        StockReceipt receipt = draftReceipt(OWNER_ID, a, 5);
        receipt.addDetail(detail(b, 8));
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        when(productRepository.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(a, b));

        ReceiptResponse response = service.confirm(RECEIPT_ID);

        assertEquals(15, a.getQuantityInStock());
        assertEquals(8, b.getQuantityInStock());
        assertEquals(DocumentStatus.CONFIRMED, response.status());
        assertEquals(NOW, response.confirmedAt());
    }

    @Test
    void confirmSecondTimeReturns409AndKeepsStock() {
        Product a = product(1L, 10);
        StockReceipt receipt = draftReceipt(OWNER_ID, a, 5);
        receipt.setStatus(DocumentStatus.CONFIRMED);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(RECEIPT_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(10, a.getQuantityInStock());
        verify(productRepository, never()).findAllByIdForUpdate(anyCollection());
    }

    @Test
    void confirmCancelledReceiptReturns409() {
        StockReceipt receipt = draftReceipt(OWNER_ID, product(1L, 0), 5);
        receipt.setStatus(DocumentStatus.CANCELLED);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(RECEIPT_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertNull(receipt.getConfirmedAt());
    }

    @Test
    void confirmByOtherStaffReturns403BeforeTouchingStock() {
        authenticateAs(OTHER_ID, Role.STAFF);
        Product a = product(1L, 10);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID))
                .thenReturn(Optional.of(draftReceipt(OWNER_ID, a, 5)));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(RECEIPT_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(10, a.getQuantityInStock());
    }

    @Test
    void confirmByAdminOnOthersReceiptSucceeds() {
        authenticateAs(OTHER_ID, Role.ADMIN);
        Product a = product(1L, 1);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID))
                .thenReturn(Optional.of(draftReceipt(OWNER_ID, a, 5)));
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(a));

        ReceiptResponse response = service.confirm(RECEIPT_ID);

        assertNotNull(response.confirmedAt());
        assertEquals(6, a.getQuantityInStock());
    }

    @Test
    void confirmReturns404WhenReceiptMissing() {
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(RECEIPT_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void confirmRejectsStockOverflowWith409() {
        Product a = product(1L, Integer.MAX_VALUE);
        StockReceipt receipt = draftReceipt(OWNER_ID, a, 1);
        when(receiptRepository.findByIdForUpdate(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(a));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirm(RECEIPT_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(DocumentStatus.DRAFT, receipt.getStatus());
    }

    // ---------- helpers ----------

    private void authenticateAs(Long id, Role role) {
        AuthUser user = new AuthUser(id, "u" + id + "@test.local", role);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private ReceiptRequest request(ReceiptLineRequest... lines) {
        return new ReceiptRequest(SUPPLIER_ID, "ghi chú", List.of(lines));
    }

    private ReceiptLineRequest line(Long productId, int quantity) {
        return new ReceiptLineRequest(productId, quantity, BigDecimal.valueOf(9000));
    }

    private Supplier supplier() {
        Supplier s = new Supplier();
        s.setId(SUPPLIER_ID);
        s.setName("NCC");
        return s;
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setFullName("User " + id);
        u.setRole(Role.STAFF);
        return u;
    }

    private Product product(Long id, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setSku("SKU-" + id);
        p.setName("Product " + id);
        p.setQuantityInStock(stock);
        p.setActive(true);
        return p;
    }

    private StockReceiptDetail detail(Product product, int quantity) {
        StockReceiptDetail d = new StockReceiptDetail();
        d.setProduct(product);
        d.setQuantity(quantity);
        d.setUnitPrice(BigDecimal.valueOf(9000));
        return d;
    }

    private StockReceipt draftReceipt(Long ownerId, Product product, int quantity) {
        StockReceipt r = new StockReceipt();
        r.setId(RECEIPT_ID);
        r.setCode("PN-20261123-001");
        r.setSupplier(supplier());
        r.setCreatedBy(user(ownerId));
        r.setStatus(DocumentStatus.DRAFT);
        r.addDetail(detail(product, quantity));
        return r;
    }
}
