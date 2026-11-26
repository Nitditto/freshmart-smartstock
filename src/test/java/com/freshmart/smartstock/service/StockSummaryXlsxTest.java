package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.freshmart.smartstock.dto.inventory.StockSummaryItem;
import com.freshmart.smartstock.service.impl.StockSummaryXlsx;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class StockSummaryXlsxTest {

    private static final List<String> REQUIRED_PARTS = List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml");

    @Test
    void packageContainsEveryPartExcelNeedsAndEachOneIsWellFormedXml() throws Exception {
        Map<String, byte[]> parts = unzip(StockSummaryXlsx.write(List.of(item(1L, "RCQ-001", "Cà chua"))));

        assertEquals(REQUIRED_PARTS.size(), parts.size());
        for (String name : REQUIRED_PARTS) {
            assertNotNull(parts.get(name), name);
            parse(parts.get(name));
        }
    }

    @Test
    void headerRowIsBoldAndDataRowsKeepNumbersAsNumericCells() throws Exception {
        Document sheet = sheet(StockSummaryXlsx.write(List.of(item(1L, "RCQ-001", "Cà chua"))));

        NodeList rows = sheet.getElementsByTagName("row");
        assertEquals(2, rows.getLength());
        List<String> header = texts((Element) rows.item(0));
        assertEquals(List.of("Mã SP", "SKU", "Tên sản phẩm", "Tồn đầu", "Nhập", "Xuất", "Tồn cuối"), header);
        assertEquals("1", ((Element) ((Element) rows.item(0)).getElementsByTagName("c").item(0)).getAttribute("s"));

        Element data = (Element) rows.item(1);
        assertEquals(List.of("RCQ-001", "Cà chua"), texts(data));
        assertEquals(List.of("1", "70", "20", "5", "85"), numbers(data));
    }

    @Test
    void emptyReportStillHasTheHeaderRow() throws Exception {
        Document sheet = sheet(StockSummaryXlsx.write(List.of()));

        assertEquals(1, sheet.getElementsByTagName("row").getLength());
    }

    @Test
    void markupInTextIsEscapedAndNeverBecomesAFormula() throws Exception {
        Document sheet = sheet(StockSummaryXlsx.write(List.of(item(2L, "=SUM(A1)", "A & B <script>"))));

        Element data = (Element) sheet.getElementsByTagName("row").item(1);
        assertEquals(List.of("=SUM(A1)", "A & B <script>"), texts(data));
        assertEquals(0, sheet.getElementsByTagName("f").getLength());
    }

    @Test
    void charactersXmlCannotCarryAreDroppedSoExcelStillOpensTheFile() throws Exception {
        Document sheet = sheet(StockSummaryXlsx.write(List.of(item(3L, "X-1", "Tên\u0000lạ\u000B"))));

        Element data = (Element) sheet.getElementsByTagName("row").item(1);
        assertEquals(List.of("X-1", "Tênlạ"), texts(data));
    }

    private static StockSummaryItem item(Long id, String sku, String name) {
        return new StockSummaryItem(id, sku, name, 70, 20, 5, 85);
    }

    private static Document sheet(byte[] xlsx) throws Exception {
        Map<String, byte[]> parts = unzip(xlsx);
        assertFalse(parts.isEmpty());
        return parse(parts.get("xl/worksheets/sheet1.xml"));
    }

    private static List<String> texts(Element row) {
        NodeList cells = row.getElementsByTagName("c");
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            if ("inlineStr".equals(cell.getAttribute("t"))) {
                out.add(cell.getElementsByTagName("t").item(0).getTextContent());
            }
        }
        return out;
    }

    private static List<String> numbers(Element row) {
        NodeList cells = row.getElementsByTagName("c");
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            if (!cell.hasAttribute("t")) {
                out.add(cell.getElementsByTagName("v").item(0).getTextContent());
            }
        }
        return out;
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private static Map<String, byte[]> unzip(byte[] xlsx) throws IOException {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                parts.put(entry.getName(), zip.readAllBytes());
            }
        }
        assertTrue(parts.size() > 0);
        return parts;
    }
}
