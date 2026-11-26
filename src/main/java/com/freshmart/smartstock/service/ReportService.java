package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.dto.report.ExportFormat;
import com.freshmart.smartstock.dto.report.ReportFile;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /** Per product: opening + in - out = closing, between two dates (inclusive), CONFIRMED documents only. */
    List<StockSummaryItem> stockSummary(LocalDate from, LocalDate to);

    /** Same report as {@link #stockSummary} as a CSV or Excel (xlsx) file; from > to => 400. */
    ReportFile exportStockSummary(LocalDate from, LocalDate to, ExportFormat format);
}
