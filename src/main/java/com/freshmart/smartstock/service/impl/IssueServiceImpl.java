package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.api.PageableGuard;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.entity.StockIssueDetail;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ShortageItem;
import com.freshmart.smartstock.dto.issue.IssueConfirmResponse;
import com.freshmart.smartstock.dto.issue.IssueLineRequest;
import com.freshmart.smartstock.dto.issue.IssueRequest;
import com.freshmart.smartstock.dto.issue.IssueResponse;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.security.SecurityUtils;
import com.freshmart.smartstock.service.DocumentCodeGenerator;
import com.freshmart.smartstock.service.IssueService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private static final Set<String> SORTABLE = Set.of("id", "code", "createdAt", "confirmedAt");

    private final StockIssueRepository issueRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DocumentCodeGenerator codeGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public IssueResponse create(IssueRequest request) {
        AuthUser me = SecurityUtils.currentUser();
        Map<Long, Product> products = loadLineProducts(request.lines());

        StockIssue issue = new StockIssue();
        issue.setCode(codeGenerator.next(DocumentCodeGenerator.ISSUE_PREFIX));
        issue.setReason(request.reason());
        issue.setNote(trimToNull(request.note()));
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(userRepository.getReferenceById(me.id()));
        addLines(issue, request.lines(), products);

        StockIssue saved = issueRepository.save(issue);
        return IssueResponse.from(issueRepository.findById(saved.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> search(DocumentStatus status, LocalDate from, LocalDate to,
                                              Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("Ngày bắt đầu (from) không được sau ngày kết thúc (to)");
        }
        Pageable safe = PageableGuard.sanitize(pageable, SORTABLE);
        Page<StockIssue> page = issueRepository.search(
                status,
                DocumentGuard.startOfDay(from, clock),
                DocumentGuard.startOfNextDay(to, clock),
                safe);
        return PageResponse.from(page, IssueResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public IssueResponse get(Long id) {
        return IssueResponse.from(issueRepository.findById(id).orElseThrow(() -> issueNotFound(id)));
    }

    @Override
    @Transactional
    public IssueResponse update(Long id, IssueRequest request) {
        AuthUser me = SecurityUtils.currentUser();
        // Lock the row first so a concurrent confirm/cancel cannot slip between the status check and the write.
        StockIssue issue = issueRepository.findByIdForUpdate(id).orElseThrow(() -> issueNotFound(id));
        DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);
        DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());
        Map<Long, Product> products = loadLineProducts(request.lines());

        issue.setReason(request.reason());
        issue.setNote(trimToNull(request.note()));
        // Flush the delete before inserting: Hibernate runs INSERT first and would break UNIQUE(issue_id, product_id).
        issue.clearDetails();
        issueRepository.saveAndFlush(issue);
        addLines(issue, request.lines(), products);

        issueRepository.save(issue);
        return IssueResponse.from(issueRepository.findById(id).orElseThrow());
    }

    /** Check order follows api-contract section 5.12: 404 -> 403 owner -> 403 DAMAGED -> 409 status -> 409 stock. */
    @Override
    @Transactional
    public IssueConfirmResponse confirm(Long id) {
        AuthUser me = SecurityUtils.currentUser();
        // Lock order: issue -> products (by id) -> check/write. See AGENTS.md section 5 (REPEATABLE READ trap).
        StockIssue issue = issueRepository.findByIdForUpdate(id).orElseThrow(() -> issueNotFound(id));
        DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);
        if (issue.getReason() == IssueReason.DAMAGED && me.role() != Role.ADMIN) {
            throw ApiException.forbidden("Chỉ ADMIN được xác nhận phiếu xuất lý do DAMAGED");
        }
        DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());

        Map<Long, Product> locked = lockProducts(issue);
        List<ShortageItem> shortages = findShortages(issue, locked);
        if (!shortages.isEmpty()) {
            throw ApiException.conflict("Không đủ tồn kho", shortages);
        }
        List<LowStockItem> newlyLow = deductStock(issue, locked);
        issue.setStatus(DocumentStatus.CONFIRMED);
        issue.setConfirmedAt(Instant.now(clock));
        return new IssueConfirmResponse(IssueResponse.from(issueRepository.findById(id).orElseThrow()), newlyLow);
    }

    @Override
    @Transactional
    public IssueResponse cancel(Long id) {
        AuthUser me = SecurityUtils.currentUser();
        StockIssue issue = issueRepository.findByIdForUpdate(id).orElseThrow(() -> issueNotFound(id));
        DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);
        DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());

        issue.setStatus(DocumentStatus.CANCELLED);
        issueRepository.save(issue);
        return IssueResponse.from(issueRepository.findById(id).orElseThrow());
    }

    private Map<Long, Product> loadLineProducts(List<IssueLineRequest> lines) {
        List<Long> ids = lines.stream().map(IssueLineRequest::productId).toList();
        DocumentGuard.assertNoDuplicateProducts(ids);
        return DocumentGuard.loadProductsForLines(ids, productRepository);
    }

    private static void addLines(StockIssue issue, List<IssueLineRequest> lines, Map<Long, Product> products) {
        for (IssueLineRequest line : lines) {
            StockIssueDetail detail = new StockIssueDetail();
            detail.setProduct(products.get(line.productId()));
            detail.setQuantity(line.quantity());
            issue.addDetail(detail);
        }
    }

    /** Ids come from the lazy line proxies, so no stale Product is loaded before the row lock is taken. */
    private Map<Long, Product> lockProducts(StockIssue issue) {
        List<Long> ids = issue.getDetails().stream()
                .map(d -> d.getProduct().getId())
                .sorted()
                .distinct()
                .toList();
        return productRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    /** Checks EVERY line before touching stock, so the user gets the full shortage list and nothing is deducted. */
    private static List<ShortageItem> findShortages(StockIssue issue, Map<Long, Product> locked) {
        List<ShortageItem> shortages = new ArrayList<>();
        for (StockIssueDetail detail : issue.getDetails()) {
            Product product = locked.get(detail.getProduct().getId());
            if (detail.getQuantity() > product.getQuantityInStock()) {
                shortages.add(new ShortageItem(product.getId(), product.getSku(), product.getName(),
                        detail.getQuantity(), product.getQuantityInStock()));
            }
        }
        return shortages;
    }

    /** Returns the products of this issue that end at or below MinStock (api-contract section 5.9). */
    private static List<LowStockItem> deductStock(StockIssue issue, Map<Long, Product> locked) {
        List<LowStockItem> newlyLow = new ArrayList<>();
        for (StockIssueDetail detail : issue.getDetails()) {
            Product product = locked.get(detail.getProduct().getId());
            product.setQuantityInStock(product.getQuantityInStock() - detail.getQuantity());
            if (product.isLowStock()) {
                newlyLow.add(LowStockItem.from(product));
            }
        }
        return newlyLow;
    }

    private static ApiException issueNotFound(Long id) {
        return ApiException.notFound("Không tìm thấy phiếu xuất " + id);
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
