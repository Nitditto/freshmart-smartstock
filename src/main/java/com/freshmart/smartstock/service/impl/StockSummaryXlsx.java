package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes the stock-summary report as a minimal .xlsx (Office Open XML) with plain java.util.zip, so no new
 * dependency is needed. Text goes in as inline strings (never formulas, so no injection), numbers as real numeric
 * cells, and the header is bold and frozen. Unlike CSV it does not depend on the list separator of the Excel locale.
 */
public final class StockSummaryXlsx {

    private static final String[] HEADERS = {"Mã SP", "SKU", "Tên sản phẩm", "Tồn đầu", "Nhập", "Xuất", "Tồn cuối"};
    private static final int[] COLUMN_WIDTHS = {8, 14, 36, 10, 10, 10, 10};
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String SHEET_CT = "application/vnd.openxmlformats-officedocument.spreadsheetml.";
    private static final int HEADER_STYLE = 1;

    private StockSummaryXlsx() {
    }

    public static byte[] write(List<StockSummaryItem> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", rootRels());
            put(zip, "xl/workbook.xml", workbook());
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            put(zip, "xl/styles.xml", styles());
            put(zip, "xl/worksheets/sheet1.xml", sheet(items));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return XML_DECLARATION
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"" + SHEET_CT + "sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"" + SHEET_CT + "worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"" + SHEET_CT + "styles+xml\"/>"
                + "</Types>";
    }

    private static String rootRels() {
        return XML_DECLARATION + "<Relationships xmlns=\"" + PKG_REL_NS + "\">"
                + "<Relationship Id=\"rId1\" Type=\"" + REL_NS + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook() {
        return XML_DECLARATION + "<workbook xmlns=\"" + MAIN_NS + "\" xmlns:r=\"" + REL_NS + "\">"
                + "<sheets><sheet name=\"Tồn kho\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    private static String workbookRels() {
        return XML_DECLARATION + "<Relationships xmlns=\"" + PKG_REL_NS + "\">"
                + "<Relationship Id=\"rId1\" Type=\"" + REL_NS + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"" + REL_NS + "/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String styles() {
        return XML_DECLARATION + "<styleSheet xmlns=\"" + MAIN_NS + "\">"
                + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
                + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    private static String sheet(List<StockSummaryItem> items) {
        StringBuilder xml = new StringBuilder(XML_DECLARATION)
                .append("<worksheet xmlns=\"").append(MAIN_NS).append("\">")
                .append("<sheetViews><sheetView workbookViewId=\"0\">")
                .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                .append("</sheetView></sheetViews><cols>");
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            xml.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(COLUMN_WIDTHS[i]).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData><row r=\"1\">");
        for (String header : HEADERS) {
            xml.append(textCell(header, HEADER_STYLE));
        }
        xml.append("</row>");
        int rowNumber = 2;
        for (StockSummaryItem item : items) {
            xml.append("<row r=\"").append(rowNumber++).append("\">")
                    .append(numberCell(item.productId()))
                    .append(textCell(item.sku(), 0))
                    .append(textCell(item.name(), 0))
                    .append(numberCell(item.openingQty()))
                    .append(numberCell(item.totalIn()))
                    .append(numberCell(item.totalOut()))
                    .append(numberCell(item.closingQty()))
                    .append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static String numberCell(Number value) {
        return "<c><v>" + value + "</v></c>";
    }

    private static String textCell(String value, int style) {
        String styleAttr = style == 0 ? "" : " s=\"" + style + "\"";
        return "<c t=\"inlineStr\"" + styleAttr + "><is><t xml:space=\"preserve\">" + escape(value) + "</t></is></c>";
    }

    /** Escapes XML markup and drops characters XML 1.0 cannot carry, which would make Excel refuse the file. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> {
            if (cp == '&') {
                out.append("&amp;");
            } else if (cp == '<') {
                out.append("&lt;");
            } else if (cp == '>') {
                out.append("&gt;");
            } else if (isXmlChar(cp)) {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }

    private static boolean isXmlChar(int cp) {
        return cp == 0x9 || cp == 0xA || cp == 0xD || (cp >= 0x20 && cp <= 0xD7FF)
                || (cp >= 0xE000 && cp <= 0xFFFD) || (cp >= 0x10000 && cp <= 0x10FFFF);
    }
}
