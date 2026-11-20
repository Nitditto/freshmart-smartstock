package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/** TODO[Member E]: implement with aggregate queries (GROUP BY product), not loops over entities. from <= to else 400. */
@Service
public class ReportServiceImpl implements ReportService {

    @Override
    public List<StockSummaryItem> stockSummary(LocalDate from, LocalDate to) {
        throw ApiException.notImplemented("Member E");
    }
}
