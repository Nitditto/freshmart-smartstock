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
        List<Long> ids = request.lines().stream().map(IssueLineRequest::productId).toList();
        DocumentGuard.assertNoDuplicateProducts(ids);
        Map<Long, Product> products = DocumentGuard.loadProductsForLines(ids, productRepository);

        StockIssue issue = new StockIssue();
        issue.setCode(codeGenerator.next(DocumentCodeGenerator.ISSUE_PREFIX));
        issue.setReason(request.reason());
        issue.setNote(trimToNull(request.note()));
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(userRepository.getReferenceById(me.id()));

        for (IssueLineRequest line : request.lines()) {
            StockIssueDetail detail = new StockIssueDetail();
            detail.setProduct(products.get(line.productId()));
            detail.setQuantity(line.quantity());
            issue.addDetail(detail);
        }

        StockIssue saved = issueRepository.save(issue);
        return IssueResponse.from(issueRepository.findById(saved.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> search(DocumentStatus status, LocalDate from, LocalDate to,
                                              Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("from phải ≤ to");
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
        StockIssue issue = issueRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu xuất " + id));
        return IssueResponse.from(issue);
    }

    @Override
    @Transactional
    public IssueResponse update(Long id, IssueRequest request) {
        AuthUser me = SecurityUtils.currentUser();
        StockIssue issue = issueRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu xuất " + id));

        DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);
        DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());

        List<Long> ids = request.lines().stream().map(IssueLineRequest::productId).toList();
        DocumentGuard.assertNoDuplicateProducts(ids);
        Map<Long, Product> products = DocumentGuard.loadProductsForLines(ids, productRepository);

        issue.setReason(request.reason());
        issue.setNote(trimToNull(request.note()));
        issue.clearDetails();
        issueRepository.saveAndFlush(issue);

        for (IssueLineRequest line : request.lines()) {
            StockIssueDetail detail = new StockIssueDetail();
            detail.setProduct(products.get(line.productId()));
            detail.setQuantity(line.quantity());
            issue.addDetail(detail);
        }

        issueRepository.save(issue);
        return IssueResponse.from(issueRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional
    public IssueConfirmResponse confirm(Long id) {
        AuthUser me = SecurityUtils.currentUser();
        StockIssue issue = issueRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu xuất " + id));

        DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);
        if (issue.getReason() == IssueReason.DAMAGED && me.role() != Role.ADMIN) {
            throw ApiException.forbidden("Chỉ ADMIN được xác nhận phiếu xuất lý do DAMAGED");
        }
        DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());

        List<Long> ids = issue.getDetails().stream()
                .map(d -> d.getProduct().getId())
                .sorted()
                .distinct()
                .toList();
        Map<Long, Product> locked = productRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ShortageItem> shortages = new ArrayList<>();
        for (StockIssueDetail detail : issue.getDetails()) {
            Product product = locked.get(detail.getProduct().getId());
            if (detail.getQuantity() > product.getQuantityInStock()) {
                shortages.add(new ShortageItem(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        detail.getQuantity(),
                        product.getQuantityInStock()));
            }
        }
        if (!shortages.isEmpty()) {
            throw ApiException.conflict("Không đủ tồn kho", shortages);
        }

        List<Product> touched = new ArrayList<>();
        for (StockIssueDetail detail : issue.getDetails()) {
            Product product = locked.get(detail.getProduct().getId());
            product.setQuantityInStock(product.getQuantityInStock() - detail.getQuantity());
            touched.add(product);
        }

        issue.setStatus(DocumentStatus.CONFIRMED);
        issue.setConfirmedAt(Instant.now(clock));

        List<LowStockItem> newlyLow = touched.stream()
                .filter(Product::isLowStock)
                .map(LowStockItem::from)
                .toList();

        return new IssueConfirmResponse(
                IssueResponse.from(issueRepository.findById(id).orElseThrow()),
                newlyLow);
    }

    @Override
    @Transactional
    public IssueResponse cancel(Long id) {
        AuthUser me = SecurityUtils.currentUser();
        StockIssue issue = issueRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu xuất " + id));

        DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);
        DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());

        issue.setStatus(DocumentStatus.CANCELLED);
        issueRepository.save(issue);
        return IssueResponse.from(issueRepository.findById(id).orElseThrow());
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
