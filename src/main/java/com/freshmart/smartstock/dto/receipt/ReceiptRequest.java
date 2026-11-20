package com.freshmart.smartstock.dto.receipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Used by POST /api/receipts and PUT /api/receipts/{id}. Creator is taken from the JWT, never from here. */
public record ReceiptRequest(
        @NotNull Long supplierId,
        @Size(max = 500) String note,
        @NotEmpty(message = "Phiếu phải có ít nhất 1 dòng") List<@Valid ReceiptLineRequest> lines) {
}
