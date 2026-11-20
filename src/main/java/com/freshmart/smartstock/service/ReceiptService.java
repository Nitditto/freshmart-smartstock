package com.freshmart.smartstock.service;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.receipt.ReceiptRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

/**
 * Goods receipts. Rules (see docs/api-contract.md):
 * - create/update only DRAFT; STAFF may update/confirm/cancel only their OWN documents (ADMIN: any)
 * - confirm: DRAFT -> CONFIRMED and, in ONE transaction, stock += quantity for every line
 * - changing a CONFIRMED/CANCELLED document => 409
 */
public interface ReceiptService {

    ReceiptResponse create(ReceiptRequest request);

    PageResponse<ReceiptResponse> search(DocumentStatus status, LocalDate from, LocalDate to, Pageable pageable);

    ReceiptResponse get(Long id);

    ReceiptResponse update(Long id, ReceiptRequest request);

    ReceiptResponse confirm(Long id);

    ReceiptResponse cancel(Long id);
}
