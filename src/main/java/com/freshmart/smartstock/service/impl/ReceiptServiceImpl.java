package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.receipt.ReceiptRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptResponse;
import com.freshmart.smartstock.service.ReceiptService;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * TODO[Member D]: implement. Building blocks already provided:
 * DocumentCodeGenerator (PN-yyyyMMdd-xxx), ProductRepository.findAllByIdForUpdate (row locks),
 * StockReceiptRepository (EntityGraph findById + search), SecurityUtils.currentUser().
 * confirm() MUST be @Transactional and lock products before changing stock.
 */
@Service
public class ReceiptServiceImpl implements ReceiptService {

    @Override
    public ReceiptResponse create(ReceiptRequest request) {
        throw ApiException.notImplemented("Member D");
    }

    @Override
    public PageResponse<ReceiptResponse> search(DocumentStatus status, LocalDate from, LocalDate to,
                                                Pageable pageable) {
        throw ApiException.notImplemented("Member D");
    }

    @Override
    public ReceiptResponse get(Long id) {
        throw ApiException.notImplemented("Member D");
    }

    @Override
    public ReceiptResponse update(Long id, ReceiptRequest request) {
        throw ApiException.notImplemented("Member D");
    }

    @Override
    public ReceiptResponse confirm(Long id) {
        throw ApiException.notImplemented("Member D");
    }

    @Override
    public ReceiptResponse cancel(Long id) {
        throw ApiException.notImplemented("Member D");
    }
}
