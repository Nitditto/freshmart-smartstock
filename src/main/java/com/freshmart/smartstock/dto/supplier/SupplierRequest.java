package com.freshmart.smartstock.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 30) String phone,
        @Size(max = 255) String address) {
}
