package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.service.impl.StockSummaryCsv;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockSummaryCsvTest {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Test
    void startsWithBomThenHeaderAndUsesCrlf() {
        byte[] csv = StockSummaryCsv.write(List.of(item("RCQ-001", "Cà chua")));

        assertArrayEquals(BOM, Arrays.copyOf(csv, 3));
        assertEquals("Mã SP,SKU,Tên sản phẩm,Tồn đầu,Nhập,Xuất,Tồn cuối\r\n1,RCQ-001,Cà chua,70,20,5,85\r\n",
                body(csv));
    }

    @Test
    void emptyReportStillHasTheHeader() {
        assertEquals("Mã SP,SKU,Tên sản phẩm,Tồn đầu,Nhập,Xuất,Tồn cuối\r\n", body(StockSummaryCsv.write(List.of())));
    }

    @Test
    void textWithCommaQuoteOrNewlineIsQuoted() {
        String row = rowOf(item("SKU-1", "Gạo \"ST25\", 5kg\nloại 1"));

        assertEquals("1,SKU-1,\"Gạo \"\"ST25\"\", 5kg\nloại 1\",70,20,5,85", row);
    }

    @Test
    void formulaTriggersAreNeutralizedInTextCells() {
        assertEquals("1,A,\"'=HYPERLINK(\"\"x\"\")\",70,20,5,85", rowOf(item("A", "=HYPERLINK(\"x\")")));
        assertEquals("'+cmd", rowOf(item("A", "+cmd")).split(",")[2]);
        assertEquals("'-1+2", rowOf(item("A", "-1+2")).split(",")[2]);
        assertEquals("'@SUM(A1)", rowOf(item("A", "@SUM(A1)")).split(",")[2]);
        assertEquals("'=1", rowOf(item("=1", "Bình thường")).split(",")[1]);
    }

    private static StockSummaryItem item(String sku, String name) {
        return new StockSummaryItem(1L, sku, name, 70, 20, 5, 85);
    }

    private static String body(byte[] csv) {
        return new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
    }

    /** The data row of a one-item report, without the trailing CRLF. */
    private static String rowOf(StockSummaryItem item) {
        String body = body(StockSummaryCsv.write(List.of(item)));
        return body.substring(body.indexOf("\r\n") + 2, body.length() - 2);
    }
}
