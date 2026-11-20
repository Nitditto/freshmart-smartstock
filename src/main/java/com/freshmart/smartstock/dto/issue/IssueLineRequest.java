package com.freshmart.smartstock.dto.issue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record IssueLineRequest(
        @NotNull Long productId,
        @NotNull @Min(value = 1, message = "Quantity phải > 0") Integer quantity) {
}
