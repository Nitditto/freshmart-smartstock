package com.freshmart.smartstock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.dto.issue.IssueRequest;
import com.freshmart.smartstock.dto.issue.IssueResponse;
import com.freshmart.smartstock.dto.receipt.ReceiptRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptResponse;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.security.SecurityUtils;
import com.freshmart.smartstock.service.IssueService;
import com.freshmart.smartstock.service.ReceiptService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SampleDocumentSeederTest {

    @Mock
    private ReceiptService receiptService;
    @Mock
    private IssueService issueService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private StockReceiptRepository receiptRepository;
    @Mock
    private StockIssueRepository issueRepository;

    @InjectMocks
    private SampleDocumentSeeder seeder;

    @Test
    void hasDocumentsWhenEitherLedgerIsNotEmpty() {
        when(receiptRepository.count()).thenReturn(0L);
        when(issueRepository.count()).thenReturn(1L);

        assertTrue(seeder.hasDocuments());
    }

    @Test
    void hasNoDocumentsOnAnEmptyLedger() {
        when(receiptRepository.count()).thenReturn(0L);
        when(issueRepository.count()).thenReturn(0L);

        assertFalse(seeder.hasDocuments());
    }

    @Test
    void seedCreatesTenDocumentsAsTheirOwnersAndAppliesEachOutcome() {
        stubMasterData();
        List<String> receiptCreators = new ArrayList<>();
        List<AuthUser> issueCreators = new ArrayList<>();
        when(receiptService.create(any(ReceiptRequest.class))).thenAnswer(inv -> {
            receiptCreators.add(SecurityUtils.currentUser().email());
            return receipt(receiptCreators.size());
        });
        when(issueService.create(any(IssueRequest.class))).thenAnswer(inv -> {
            issueCreators.add(SecurityUtils.currentUser());
            return issue(issueCreators.size());
        });

        seeder.seed();

        assertEquals(List.of(DataSeeder.STAFF1_EMAIL, DataSeeder.STAFF1_EMAIL, DataSeeder.STAFF2_EMAIL,
                DataSeeder.STAFF2_EMAIL, DataSeeder.ADMIN_EMAIL, DataSeeder.STAFF1_EMAIL, DataSeeder.STAFF2_EMAIL),
                receiptCreators);
        assertEquals(3, issueCreators.size());
        assertEquals(Role.STAFF, issueCreators.get(2).role());
        verify(receiptService, times(5)).confirm(anyLong());
        verify(receiptService, times(1)).cancel(anyLong());
        verify(issueService, times(2)).confirm(anyLong());
        verify(issueService, times(0)).cancel(anyLong());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void seedNeverSendsLinesWithUnknownProductsOrSuppliers() {
        stubMasterData();
        List<ReceiptRequest> receipts = new ArrayList<>();
        List<IssueRequest> issues = new ArrayList<>();
        when(receiptService.create(any(ReceiptRequest.class))).thenAnswer(inv -> {
            receipts.add(inv.getArgument(0));
            return receipt(receipts.size());
        });
        when(issueService.create(any(IssueRequest.class))).thenAnswer(inv -> {
            issues.add(inv.getArgument(0));
            return issue(issues.size());
        });

        seeder.seed();

        assertTrue(receipts.stream().allMatch(r -> r.supplierId() != null
                && r.lines().stream().allMatch(l -> l.productId() != null && l.unitPrice() != null)));
        assertTrue(issues.stream().allMatch(i -> i.lines().stream().allMatch(l -> l.productId() != null)));
        assertEquals(IssueReason.DAMAGED, issues.get(2).reason());
    }

    private void stubMasterData() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, DataSeeder.ADMIN_EMAIL, Role.ADMIN),
                user(2L, DataSeeder.STAFF1_EMAIL, Role.STAFF),
                user(3L, DataSeeder.STAFF2_EMAIL, Role.STAFF)));
        List<Product> products = new ArrayList<>();
        Stream.of("RCQ", "TCA", "STR", "DKH", "DUO").forEach(prefix -> {
            for (int i = 1; i <= 6; i++) {
                products.add(product(products.size() + 1L, "%s-%03d".formatted(prefix, i)));
            }
        });
        when(productRepository.findAll()).thenReturn(products);
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(1L, DataSeeder.SUPPLIER_FARM),
                supplier(2L, DataSeeder.SUPPLIER_FRESH),
                supplier(3L, DataSeeder.SUPPLIER_DRINKS)));
    }

    private static User user(Long id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    private static Product product(Long id, String sku) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        return p;
    }

    private static Supplier supplier(Long id, String name) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private static ReceiptResponse receipt(long id) {
        return new ReceiptResponse(id, null, null, null, null, null, null, null, null, null, null);
    }

    private static IssueResponse issue(long id) {
        return new IssueResponse(id, null, null, null, null, null, null, null, null, null);
    }
}
