package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.dto.supplier.SupplierRequest;
import com.freshmart.smartstock.dto.supplier.SupplierResponse;
import com.freshmart.smartstock.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @Operation(summary = "List all suppliers")
    public ApiResponse<List<SupplierResponse>> findAll() {
        return ApiResponse.ok(supplierService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a supplier (ADMIN)")
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.created(supplierService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a supplier (ADMIN)")
    public ApiResponse<SupplierResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(supplierService.update(id, request));
    }
}
