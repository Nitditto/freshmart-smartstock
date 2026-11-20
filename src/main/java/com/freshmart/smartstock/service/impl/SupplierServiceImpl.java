package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.supplier.SupplierRequest;
import com.freshmart.smartstock.dto.supplier.SupplierResponse;
import com.freshmart.smartstock.service.SupplierService;
import java.util.List;
import org.springframework.stereotype.Service;

/** TODO[Member B]: implement (mirror CategoryServiceImpl). */
@Service
public class SupplierServiceImpl implements SupplierService {

    @Override
    public List<SupplierResponse> findAll() {
        throw ApiException.notImplemented("Member B");
    }

    @Override
    public SupplierResponse create(SupplierRequest request) {
        throw ApiException.notImplemented("Member B");
    }

    @Override
    public SupplierResponse update(Long id, SupplierRequest request) {
        throw ApiException.notImplemented("Member B");
    }
}
