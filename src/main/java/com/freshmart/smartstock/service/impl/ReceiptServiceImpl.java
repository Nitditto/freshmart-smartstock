package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.api.PageableGuard;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.receipt.ReceiptLineRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptResponse;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.security.SecurityUtils;
import com.freshmart.smartstock.service.DocumentCodeGenerator;
import com.freshmart.smartstock.service.ReceiptService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final Set<String> SORTABLE = Set.of("id", "code", "createdAt", "confirmedAt");

    private final StockReceiptRepository receiptRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DocumentCodeGenerator codeGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public ReceiptResponse create(ReceiptRequest request) {
        AuthUser me = SecurityUtils.currentUser();
        Supplier supplier = findSupplier(request.supplierId());
        Map<Long, Product> products = loadLineProducts(request.lines());

        StockReceipt receipt = new StockReceipt();
        receipt.setCode(codeGenerator.next(DocumentCodeGenerator.RECEIPT_PREFIX));
        receipt.setCreatedBy(userRepository.getReferenceById(me.id()));
        receipt.setSupplier(supplier);
        receipt.setNote(request.note());
        addLines(receipt, request.lines(), products);
        return ReceiptResponse.from(receiptRepository.save(receipt));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReceiptResponse> search(DocumentStatus status, LocalDate from, LocalDate to,
                                                Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("Ngày bắt đầu (from) không được sau ngày kết thúc (to)");
        }
        Pageable safe = PageableGuard.sanitize(pageable, SORTABLE);
        return PageResponse.from(
                receiptRepository.search(status, DocumentGuard.startOfDay(from, clock),
                        DocumentGuard.startOfNextDay(to, clock), safe),
                ReceiptResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse get(Long id) {
        return ReceiptResponse.from(receiptRepository.findById(id).orElseThrow(() -> receiptNotFound(id)));
    }

    @Override
    @Transactional
    public ReceiptResponse update(Long id, ReceiptRequest request) {
        AuthUser me = SecurityUtils.currentUser();
        // Lock the row first so a concurrent confirm/cancel cannot slip between the status check and the write.
        StockReceipt receipt = receiptRepository.findByIdForUpdate(id).orElseThrow(() -> receiptNotFound(id));
        DocumentGuard.assertCanModify(receipt.getCreatedBy().getId(), me);
        DocumentGuard.assertDraft(receipt.getStatus(), receipt.getCode());

        Supplier supplier = findSupplier(request.supplierId());
        Map<Long, Product> products = loadLineProducts(request.lines());

        receipt.setSupplier(supplier);
        receipt.setNote(request.note());
        // Flush the delete before inserting: Hibernate runs INSERT first and would break
        // UNIQUE(receipt_id, product_id).
        receipt.clearDetails();
        receiptRepository.saveAndFlush(receipt);
        addLines(receipt, request.lines(), products);
        return ReceiptResponse.from(receiptRepository.save(receipt));
    }

    @Override
    @Transactional
    public ReceiptResponse confirm(Long id) {
        AuthUser me = SecurityUtils.currentUser();
        // Lock order: receipt -> products (by id) -> check/write. See AGENTS.md section 5 (REPEATABLE READ trap).
        StockReceipt receipt = receiptRepository.findByIdForUpdate(id).orElseThrow(() -> receiptNotFound(id));
        DocumentGuard.assertCanModify(receipt.getCreatedBy().getId(), me);
        DocumentGuard.assertDraft(receipt.getStatus(), receipt.getCode());

        List<Long> productIds = receipt.getDetails().stream().map(d -> d.getProduct().getId()).sorted().toList();
        Map<Long, Product> locked = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (StockReceiptDetail detail : receipt.getDetails()) {
            increaseStock(locked.get(detail.getProduct().getId()), detail.getQuantity());
        }
        receipt.setStatus(DocumentStatus.CONFIRMED);
        receipt.setConfirmedAt(Instant.now(clock));
        return ReceiptResponse.from(receipt);
    }

    @Override
    @Transactional
    public ReceiptResponse cancel(Long id) {
        AuthUser me = SecurityUtils.currentUser();
        StockReceipt receipt = receiptRepository.findByIdForUpdate(id).orElseThrow(() -> receiptNotFound(id));
        DocumentGuard.assertCanModify(receipt.getCreatedBy().getId(), me);
        DocumentGuard.assertDraft(receipt.getStatus(), receipt.getCode());
        receipt.setStatus(DocumentStatus.CANCELLED);
        return ReceiptResponse.from(receipt);
    }

    private Supplier findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy nhà cung cấp " + supplierId));
    }

    private Map<Long, Product> loadLineProducts(List<ReceiptLineRequest> lines) {
        List<Long> productIds = lines.stream().map(ReceiptLineRequest::productId).toList();
        DocumentGuard.assertNoDuplicateProducts(productIds);
        return DocumentGuard.loadProductsForLines(productIds, productRepository);
    }

    private void addLines(StockReceipt receipt, List<ReceiptLineRequest> lines, Map<Long, Product> products) {
        for (ReceiptLineRequest line : lines) {
            StockReceiptDetail detail = new StockReceiptDetail();
            detail.setProduct(products.get(line.productId()));
            detail.setQuantity(line.quantity());
            detail.setUnitPrice(line.unitPrice());
            receipt.addDetail(detail);
        }
    }

    private void increaseStock(Product product, int quantity) {
        try {
            product.setQuantityInStock(Math.addExact(product.getQuantityInStock(), quantity));
        } catch (ArithmeticException e) {
            throw ApiException.conflict("Tồn kho của sản phẩm '" + product.getName() + "' vượt giới hạn cho phép");
        }
    }

    private ApiException receiptNotFound(Long id) {
        return ApiException.notFound("Không tìm thấy phiếu nhập " + id);
    }
}
