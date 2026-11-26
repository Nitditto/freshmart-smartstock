package com.freshmart.smartstock.config;

import com.freshmart.smartstock.config.SampleDocuments.IssuePlan;
import com.freshmart.smartstock.config.SampleDocuments.ReceiptPlan;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.issue.IssueLineRequest;
import com.freshmart.smartstock.dto.issue.IssueRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptLineRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptRequest;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.service.IssueService;
import com.freshmart.smartstock.service.ReceiptService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Seed part 2: creates the sample documents through the REAL ReceiptService/IssueService (agreed exception to the
 * "no cross-feature service call" rule, see docs/plans/00-shared.md section 4.3). Going through the services means
 * every unit of stock is explained by a confirmed document, so history and stock-summary agree with the stock.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
class SampleDocumentSeeder {

    private final ReceiptService receiptService;
    private final IssueService issueService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockReceiptRepository receiptRepository;
    private final StockIssueRepository issueRepository;

    /** Documents are only seeded into an empty ledger, so restarting the app never doubles stock. */
    boolean hasDocuments() {
        return receiptRepository.count() > 0 || issueRepository.count() > 0;
    }

    void seed() {
        Map<String, User> users = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getEmail, Function.identity()));
        Map<String, Long> productIds = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getSku, Product::getId));
        Map<String, Long> supplierIds = supplierRepository.findAll().stream()
                .collect(Collectors.toMap(Supplier::getName, Supplier::getId));

        for (ReceiptPlan plan : SampleDocuments.RECEIPTS) {
            runAs(users.get(plan.creatorEmail()), () -> seedReceipt(plan, productIds, supplierIds));
        }
        for (IssuePlan plan : SampleDocuments.ISSUES) {
            runAs(users.get(plan.creatorEmail()), () -> seedIssue(plan, productIds));
        }
        log.info("Seeded {} sample documents", SampleDocuments.RECEIPTS.size() + SampleDocuments.ISSUES.size());
    }

    private void seedReceipt(ReceiptPlan plan, Map<String, Long> productIds, Map<String, Long> supplierIds) {
        List<ReceiptLineRequest> lines = plan.lines().stream()
                .map(line -> new ReceiptLineRequest(productIds.get(line.sku()), line.quantity(), line.unitPrice()))
                .toList();
        Long id = receiptService.create(new ReceiptRequest(supplierIds.get(plan.supplierName()), plan.note(), lines))
                .id();
        if (plan.outcome() == DocumentStatus.CONFIRMED) {
            receiptService.confirm(id);
        } else if (plan.outcome() == DocumentStatus.CANCELLED) {
            receiptService.cancel(id);
        }
    }

    private void seedIssue(IssuePlan plan, Map<String, Long> productIds) {
        List<IssueLineRequest> lines = plan.lines().stream()
                .map(line -> new IssueLineRequest(productIds.get(line.sku()), line.quantity()))
                .toList();
        Long id = issueService.create(new IssueRequest(plan.reason(), plan.note(), lines)).id();
        if (plan.outcome() == DocumentStatus.CONFIRMED) {
            issueService.confirm(id);
        } else if (plan.outcome() == DocumentStatus.CANCELLED) {
            issueService.cancel(id);
        }
    }

    /**
     * Services take the caller from the SecurityContext (never from input), and a startup runner has no request,
     * so each document is created "as" its seed user, exactly like the JWT filter would do.
     */
    private static void runAs(User user, Runnable action) {
        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getRole());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
        SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
