package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.supplier.SupplierRequest;
import com.freshmart.smartstock.dto.supplier.SupplierResponse;
import java.util.List;

public interface SupplierService {

    List<SupplierResponse> findAll();

    SupplierResponse create(SupplierRequest request);

    /** 404 if missing. */
    SupplierResponse update(Long id, SupplierRequest request);
}
