package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.dto.report.CsvFile;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /** Per product: opening + in - out = closing, between two dates (inclusive), CONFIRMED documents only. */
    List<StockSummaryItem> stockSummary(LocalDate from, LocalDate to);

    /** Same report as {@link #stockSummary} as a CSV file for Excel; from > to => 400. */
    CsvFile exportStockSummaryCsv(LocalDate from, LocalDate to);
}
