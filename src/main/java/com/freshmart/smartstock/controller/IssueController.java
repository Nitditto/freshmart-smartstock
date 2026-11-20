package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.dto.issue.IssueConfirmResponse;
import com.freshmart.smartstock.dto.issue.IssueRequest;
import com.freshmart.smartstock.dto.issue.IssueResponse;
import com.freshmart.smartstock.service.IssueService;
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
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Goods issues (phiếu xuất)")
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a multi-line DRAFT issue (creator = token user)")
    public ApiResponse<IssueResponse> create(@Valid @RequestBody IssueRequest request) {
        return ApiResponse.created(issueService.create(request));
    }

    @GetMapping
    @Operation(summary = "List issues (filter by status and from/to date)")
    public ApiResponse<PageResponse<IssueResponse>> search(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.ok(issueService.search(status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one issue with its lines")
    public ApiResponse<IssueResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(issueService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT issue (owner or ADMIN)")
    public ApiResponse<IssueResponse> update(@PathVariable Long id, @Valid @RequestBody IssueRequest request) {
        return ApiResponse.ok(issueService.update(id, request));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm a DRAFT issue -> stock decreases; 409 + shortage list if stock is insufficient")
    public ApiResponse<IssueConfirmResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(issueService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a DRAFT issue (owner or ADMIN)")
    public ApiResponse<IssueResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(issueService.cancel(id));
    }
}
