package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.dto.inventory.ProductHistoryItem;
import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.service.InventoryService;
import com.freshmart.smartstock.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Low stock, product history, stock-summary report")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ReportService reportService;

    @GetMapping("/api/inventory/low-stock")
    @Operation(summary = "Products with stock <= minimum stock")
    public ApiResponse<List<LowStockItem>> lowStock() {
        return ApiResponse.ok(inventoryService.lowStock());
    }

    @GetMapping("/api/products/{id}/history")
    @Operation(summary = "Stock-in / stock-out history of a product (confirmed documents only)")
    public ApiResponse<List<ProductHistoryItem>> history(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.history(id));
    }

    @GetMapping("/api/reports/stock-summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stock movement report between two dates (ADMIN)")
    public ApiResponse<List<StockSummaryItem>> stockSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.stockSummary(from, to));
    }
}
