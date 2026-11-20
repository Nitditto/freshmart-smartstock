package com.freshmart.smartstock.dto.supplier;

import com.freshmart.smartstock.domain.entity.Supplier;

public record SupplierResponse(Long id, String name, String phone, String address) {

    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getPhone(), s.getAddress());
    }
}
