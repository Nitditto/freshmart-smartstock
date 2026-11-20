package com.freshmart.smartstock.dto.receipt;

import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import java.time.Instant;
import java.util.List;

public record ReceiptResponse(
        Long id,
        String code,
        Long supplierId,
        String supplierName,
        DocumentStatus status,
        String note,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant confirmedAt,
        List<ReceiptLineResponse> lines) {

    /** Requires details/product/supplier/createdBy to be loaded (repository.findById uses an EntityGraph). */
    public static ReceiptResponse from(StockReceipt r) {
        return new ReceiptResponse(
                r.getId(), r.getCode(),
                r.getSupplier().getId(), r.getSupplier().getName(),
                r.getStatus(), r.getNote(),
                r.getCreatedBy().getId(), r.getCreatedBy().getFullName(),
                r.getCreatedAt(), r.getConfirmedAt(),
                r.getDetails().stream().map(ReceiptLineResponse::from).toList());
    }
}
