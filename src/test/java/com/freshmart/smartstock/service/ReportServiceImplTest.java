package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.dto.report.CsvFile;
import com.freshmart.smartstock.repository.ReportRepository;
import com.freshmart.smartstock.repository.ReportRepository.ProductMovement;
import com.freshmart.smartstock.service.impl.ReportServiceImpl;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 20);
    private static final LocalDate TO = LocalDate.of(2026, 11, 22);
    /** 00:00 on 20/11 and 00:00 on 23/11 in Asia/Ho_Chi_Minh (UTC+7). */
    private static final Instant START = Instant.parse("2026-11-19T17:00:00Z");
    private static final Instant END = Instant.parse("2026-11-22T17:00:00Z");

    @Mock
    private ReportRepository reportRepository;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new ReportServiceImpl(reportRepository, clock);
    }

    @Test
    void stockSummaryRejectsFromAfterToWith400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.stockSummary(TO, FROM));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void stockSummaryComputesOpeningAndClosingFromConfirmedMovements() {
        when(reportRepository.sumConfirmedReceipts(START, END)).thenReturn(List.of(movement(1L, 100, 20)));
        when(reportRepository.sumConfirmedIssues(START, END)).thenReturn(List.of(movement(1L, 30, 5)));
        when(reportRepository.findAllProductsOrderById()).thenReturn(List.of(product(1L, "RCQ-001")));

        List<StockSummaryItem> items = service.stockSummary(FROM, TO);

        assertEquals(List.of(new StockSummaryItem(1L, "RCQ-001", "Product RCQ-001", 70, 20, 5, 85)), items);
    }

    @Test
    void stockSummaryListsProductsWithoutMovementAsZeros() {
        when(reportRepository.sumConfirmedReceipts(START, END)).thenReturn(List.of(movement(2L, 0, 12)));
        when(reportRepository.sumConfirmedIssues(START, END)).thenReturn(List.of());
        when(reportRepository.findAllProductsOrderById())
                .thenReturn(List.of(product(1L, "RCQ-001"), product(2L, "RCQ-002")));

        List<StockSummaryItem> items = service.stockSummary(FROM, FROM.plusDays(2));

        assertEquals(new StockSummaryItem(1L, "RCQ-001", "Product RCQ-001", 0, 0, 0, 0), items.get(0));
        assertEquals(new StockSummaryItem(2L, "RCQ-002", "Product RCQ-002", 0, 12, 0, 12), items.get(1));
    }

    @Test
    void stockSummaryAcceptsASingleDayPeriod() {
        Instant nextDay = Instant.parse("2026-11-20T17:00:00Z");
        when(reportRepository.sumConfirmedReceipts(START, nextDay)).thenReturn(List.of(movement(1L, 4, 10)));
        when(reportRepository.sumConfirmedIssues(START, nextDay)).thenReturn(List.of(movement(1L, 0, 3)));
        when(reportRepository.findAllProductsOrderById()).thenReturn(List.of(product(1L, "RCQ-001")));

        List<StockSummaryItem> items = service.stockSummary(FROM, FROM);

        assertEquals(new StockSummaryItem(1L, "RCQ-001", "Product RCQ-001", 4, 10, 3, 11), items.get(0));
    }

    @Test
    void exportUsesTheSameReportAndNamesTheFileAfterThePeriod() {
        when(reportRepository.sumConfirmedReceipts(START, END)).thenReturn(List.of(movement(1L, 100, 20)));
        when(reportRepository.sumConfirmedIssues(START, END)).thenReturn(List.of(movement(1L, 30, 5)));
        when(reportRepository.findAllProductsOrderById()).thenReturn(List.of(product(1L, "RCQ-001")));

        CsvFile file = service.exportStockSummaryCsv(FROM, TO);

        assertEquals("stock-summary_2026-11-20_2026-11-22.csv", file.fileName());
        String csv = new String(file.content(), StandardCharsets.UTF_8);
        assertTrue(csv.endsWith("1,RCQ-001,Product RCQ-001,70,20,5,85\r\n"));
    }

    @Test
    void exportRejectsFromAfterToWith400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.exportStockSummaryCsv(TO, FROM));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(reportRepository);
    }

    private static ProductMovement movement(Long productId, long before, long within) {
        return new ProductMovement() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getBeforeQty() {
                return before;
            }

            @Override
            public Long getWithinQty() {
                return within;
            }
        };
    }

    private static Product product(Long id, String sku) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName("Product " + sku);
        return p;
    }
}
