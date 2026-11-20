package com.freshmart.smartstock.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** QuantityInStock is intentionally absent: stock can only change through confirmed documents. */
public record ProductRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 200) String name,
        @NotNull Long categoryId,
        @NotBlank @Size(max = 30) String unit,
        @NotNull @DecimalMin(value = "0.0", message = "Giá phải >= 0") BigDecimal price,
        @NotNull @Min(value = 0, message = "MinStock phải >= 0") Integer minStock) {
}
