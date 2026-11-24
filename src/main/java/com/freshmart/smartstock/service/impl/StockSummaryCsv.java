package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes the stock-summary report as RFC 4180 CSV for Excel: UTF-8 with a BOM (otherwise Excel shows Vietnamese
 * as mojibake), CRLF line ends, and text cells protected against CSV/formula injection.
 */
public final class StockSummaryCsv {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String LINE_END = "\r\n";
    private static final String HEADER = "Mã SP,SKU,Tên sản phẩm,Tồn đầu,Nhập,Xuất,Tồn cuối";

    private StockSummaryCsv() {
    }

    public static byte[] write(List<StockSummaryItem> items) {
        StringBuilder csv = new StringBuilder(HEADER).append(LINE_END);
        for (StockSummaryItem item : items) {
            csv.append(item.productId()).append(',')
                    .append(textCell(item.sku())).append(',')
                    .append(textCell(item.name())).append(',')
                    .append(item.openingQty()).append(',')
                    .append(item.totalIn()).append(',')
                    .append(item.totalOut()).append(',')
                    .append(item.closingQty()).append(LINE_END);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(UTF8_BOM);
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /**
     * A product name such as "=HYPERLINK(...)" would run as a formula when an admin opens the file, so text that
     * starts with a formula trigger gets a leading apostrophe. Numbers are written raw and are never affected.
     */
    static String textCell(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        boolean needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        return needsQuotes ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
