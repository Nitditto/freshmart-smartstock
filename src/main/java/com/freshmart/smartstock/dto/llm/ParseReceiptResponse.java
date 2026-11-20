package com.freshmart.smartstock.dto.llm;

import java.util.List;

/**
 * SUGGESTION ONLY. Nothing is saved; the client reviews it and then calls POST /api/receipts.
 * `suggestedSupplierId` is filled only when the supplier name matches an existing supplier exactly enough.
 */
public record ParseReceiptResponse(
        Long suggestedSupplierId,
        String supplierNameInText,
        List<ParsedReceiptLine> lines,
        List<UnmatchedLine> unmatched,
        boolean mock) {
}
