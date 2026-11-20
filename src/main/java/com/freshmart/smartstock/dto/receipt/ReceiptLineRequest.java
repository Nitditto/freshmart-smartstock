package com.freshmart.smartstock.dto.receipt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReceiptLineRequest(
        @NotNull Long productId,
        @NotNull @Min(value = 1, message = "Quantity phải > 0") Integer quantity,
        @NotNull @DecimalMin(value = "0.0", message = "UnitPrice phải >= 0") BigDecimal unitPrice) {
}
