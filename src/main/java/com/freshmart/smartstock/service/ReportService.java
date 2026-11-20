package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /** Per product: opening + in - out = closing, between two dates (inclusive), CONFIRMED documents only. */
    List<StockSummaryItem> stockSummary(LocalDate from, LocalDate to);
}
