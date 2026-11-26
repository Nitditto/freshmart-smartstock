package com.freshmart.smartstock.config;

import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import java.math.BigDecimal;
import java.util.List;

/**
 * The 10 demo documents (spec: 10 receipts/issues). Quantities are chosen so that, after seeding, exactly four
 * products sit at or below MinStock (RCQ-001, RCQ-003, TCA-004, DUO-006) for the low-stock demo.
 */
final class SampleDocuments {

    /** One document line; unitPrice is null for issues. */
    record Line(String sku, int quantity, BigDecimal unitPrice) {
    }

    record ReceiptPlan(String creatorEmail, String supplierName, DocumentStatus outcome, String note,
                       List<Line> lines) {
    }

    record IssuePlan(String creatorEmail, IssueReason reason, DocumentStatus outcome, String note,
                     List<Line> lines) {
    }

    static final List<ReceiptPlan> RECEIPTS = List.of(
            new ReceiptPlan(DataSeeder.STAFF1_EMAIL, DataSeeder.SUPPLIER_FARM, DocumentStatus.CONFIRMED,
                    "Nhập rau củ đầu tuần", List.of(
                    in("RCQ-001", 60, 20000), in("RCQ-002", 80, 16000), in("RCQ-003", 15, 14000),
                    in("RCQ-004", 50, 12000), in("RCQ-005", 30, 26000), in("RCQ-006", 40, 50000))),
            new ReceiptPlan(DataSeeder.STAFF1_EMAIL, DataSeeder.SUPPLIER_FRESH, DocumentStatus.CONFIRMED,
                    "Nhập thịt cá tươi", List.of(
                    in("TCA-001", 40, 110000), in("TCA-002", 45, 68000), in("TCA-003", 12, 260000),
                    in("TCA-004", 4, 230000), in("TCA-005", 30, 170000), in("TCA-006", 30, 50000))),
            new ReceiptPlan(DataSeeder.STAFF2_EMAIL, DataSeeder.SUPPLIER_FRESH, DocumentStatus.CONFIRMED,
                    "Nhập sữa và trứng", List.of(
                    in("STR-001", 120, 26000), in("STR-002", 80, 22000), in("STR-003", 70, 34000),
                    in("STR-004", 25, 44000), in("STR-005", 25, 38000), in("STR-006", 60, 19000))),
            new ReceiptPlan(DataSeeder.STAFF2_EMAIL, DataSeeder.SUPPLIER_FARM, DocumentStatus.CONFIRMED,
                    "Nhập đồ khô", List.of(
                    in("DKH-001", 50, 120000), in("DKH-002", 40, 95000), in("DKH-003", 60, 43000),
                    in("DKH-004", 60, 19000), in("DKH-005", 45, 30000), in("DKH-006", 80, 6000))),
            new ReceiptPlan(DataSeeder.ADMIN_EMAIL, DataSeeder.SUPPLIER_DRINKS, DocumentStatus.CONFIRMED,
                    "Nhập đồ uống", List.of(
                    in("DUO-001", 60, 62000), in("DUO-002", 30, 180000), in("DUO-003", 150, 8000),
                    in("DUO-004", 25, 80000), in("DUO-005", 24, 350000))),
            new ReceiptPlan(DataSeeder.STAFF1_EMAIL, DataSeeder.SUPPLIER_DRINKS, DocumentStatus.DRAFT,
                    "Chờ kiểm đếm nước cam", List.of(in("DUO-006", 48, 20000))),
            new ReceiptPlan(DataSeeder.STAFF2_EMAIL, DataSeeder.SUPPLIER_FARM, DocumentStatus.CANCELLED,
                    "Hủy do nhà cung cấp giao trễ", List.of(in("RCQ-001", 20, 21000))));

    static final List<IssuePlan> ISSUES = List.of(
            new IssuePlan(DataSeeder.STAFF1_EMAIL, IssueReason.SALE, DocumentStatus.CONFIRMED,
                    "Xuất lên kệ bán lẻ", List.of(out("RCQ-001", 45), out("STR-001", 30), out("DUO-003", 40))),
            new IssuePlan(DataSeeder.STAFF2_EMAIL, IssueReason.INTERNAL, DocumentStatus.CONFIRMED,
                    "Xuất cho quầy chế biến", List.of(out("TCA-002", 5), out("DKH-003", 10))),
            new IssuePlan(DataSeeder.STAFF2_EMAIL, IssueReason.DAMAGED, DocumentStatus.DRAFT,
                    "Sữa chua hết hạn, chờ ADMIN duyệt", List.of(out("STR-002", 6))));

    private SampleDocuments() {
    }

    private static Line in(String sku, int quantity, long unitPrice) {
        return new Line(sku, quantity, BigDecimal.valueOf(unitPrice));
    }

    private static Line out(String sku, int quantity) {
        return new Line(sku, quantity, null);
    }
}
