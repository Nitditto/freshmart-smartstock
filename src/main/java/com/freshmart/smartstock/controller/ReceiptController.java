package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.llm.ParseReceiptRequest;
import com.freshmart.smartstock.dto.llm.ParseReceiptResponse;
import com.freshmart.smartstock.dto.receipt.ReceiptRequest;
import com.freshmart.smartstock.dto.receipt.ReceiptResponse;
import com.freshmart.smartstock.service.ReceiptParseService;
import com.freshmart.smartstock.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Goods receipts (phiếu nhập)")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final ReceiptParseService receiptParseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a multi-line DRAFT receipt (creator = token user)")
    public ApiResponse<ReceiptResponse> create(@Valid @RequestBody ReceiptRequest request) {
        return ApiResponse.created(receiptService.create(request));
    }

    @GetMapping
    @Operation(summary = "List receipts (filter by status and from/to date)")
    public ApiResponse<PageResponse<ReceiptResponse>> search(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.ok(receiptService.search(status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one receipt with its lines")
    public ApiResponse<ReceiptResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(receiptService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT receipt (owner or ADMIN)")
    public ApiResponse<ReceiptResponse> update(@PathVariable Long id, @Valid @RequestBody ReceiptRequest request) {
        return ApiResponse.ok(receiptService.update(id, request));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm a DRAFT receipt -> stock increases (owner or ADMIN)")
    public ApiResponse<ReceiptResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(receiptService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a DRAFT receipt (owner or ADMIN)")
    public ApiResponse<ReceiptResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(receiptService.cancel(id));
    }

    @PostMapping("/parse")
    @Operation(summary = "LLM: parse a text invoice into a SUGGESTED draft receipt (nothing is saved)")
    public ApiResponse<ParseReceiptResponse> parse(@Valid @RequestBody ParseReceiptRequest request) {
        return ApiResponse.ok(receiptParseService.parse(request));
    }
}
