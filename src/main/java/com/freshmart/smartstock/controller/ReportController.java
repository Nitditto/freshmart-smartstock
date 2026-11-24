package com.freshmart.smartstock.controller;

import com.freshmart.smartstock.dto.report.CsvFile;
import com.freshmart.smartstock.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * File downloads of reports (owner: Member E). The JSON report stays in InventoryController (Member B's file);
 * this controller only adds the export so the two owners never edit the same file.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report exports")
public class ReportController {

    private static final MediaType TEXT_CSV_UTF8 = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportService reportService;

    // No `produces = text/csv`: errors (400/403) must still be able to answer with the JSON envelope.
    @GetMapping("/stock-summary/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Download the stock movement report between two dates as CSV for Excel (ADMIN)")
    public ResponseEntity<byte[]> exportStockSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        CsvFile file = reportService.exportStockSummaryCsv(from, to);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
