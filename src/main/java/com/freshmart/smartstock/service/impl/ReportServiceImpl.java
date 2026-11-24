package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.dto.report.CsvFile;
import com.freshmart.smartstock.repository.ReportRepository;
import com.freshmart.smartstock.repository.ReportRepository.ProductMovement;
import com.freshmart.smartstock.service.ReportService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * opening = in - out before {@code from}; closing = opening + in - out within [from, to]. Day boundaries use the
 * business time zone (Asia/Ho_Chi_Minh) and {@code to} covers the whole day, like the document list filters.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<StockSummaryItem> stockSummary(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw ApiException.badRequest("Ngày bắt đầu (from) không được sau ngày kết thúc (to)");
        }
        Instant start = DocumentGuard.startOfDay(from, clock);
        Instant end = DocumentGuard.startOfNextDay(to, clock);
        Map<Long, ProductMovement> received = byProduct(reportRepository.sumConfirmedReceipts(start, end));
        Map<Long, ProductMovement> issued = byProduct(reportRepository.sumConfirmedIssues(start, end));
        return reportRepository.findAllProductsOrderById().stream()
                .map(p -> toItem(p, received.get(p.getId()), issued.get(p.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CsvFile exportStockSummaryCsv(LocalDate from, LocalDate to) {
        byte[] content = StockSummaryCsv.write(stockSummary(from, to));
        return new CsvFile("stock-summary_%s_%s.csv".formatted(from, to), content);
    }

    private static Map<Long, ProductMovement> byProduct(List<ProductMovement> movements) {
        return movements.stream().collect(Collectors.toMap(ProductMovement::getProductId, Function.identity()));
    }

    private static StockSummaryItem toItem(Product product, ProductMovement received, ProductMovement issued) {
        int opening = before(received) - before(issued);
        int totalIn = within(received);
        int totalOut = within(issued);
        return new StockSummaryItem(product.getId(), product.getSku(), product.getName(),
                opening, totalIn, totalOut, opening + totalIn - totalOut);
    }

    /** A product with no confirmed document simply has no row in the aggregate: that means 0, not missing data. */
    private static int before(ProductMovement movement) {
        return movement == null ? 0 : Math.toIntExact(movement.getBeforeQty());
    }

    private static int within(ProductMovement movement) {
        return movement == null ? 0 : Math.toIntExact(movement.getWithinQty());
    }
}
