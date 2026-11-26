package com.freshmart.smartstock.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline, deterministic stand-in used when no API key is configured. It reads the same prompt a real model gets
 * (catalog + invoice) and answers with the same JSON schema using simple rules: a line matches the catalog
 * product whose name it contains, the first number is the quantity, the number after "x"/"@"/"giá" the unit price.
 * Demo and CI therefore exercise the whole parse pipeline without network, key or cost.
 */
public class MockLlmService implements LlmService {

    private static final Pattern CATALOG_ROW =
            Pattern.compile("^(\\d+) \\| [^|]+ \\| ([^|]+?) \\| (.+)$", Pattern.MULTILINE);
    private static final Pattern INVOICE_BLOCK = Pattern.compile("<<<\\R(.*)\\R>>>", Pattern.DOTALL);
    private static final Pattern SEGMENT_SEPARATOR = Pattern.compile("[;\\n]");
    private static final Pattern QUANTITY = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(\\p{L}+)?");
    /** Thousands separators are "." or "," followed by exactly three digits (26.000, 1,250,000). */
    private static final Pattern PRICE =
            Pattern.compile("(?:\\bx|×|@|\\bgia)\\s*:?\\s*(\\d{1,3}(?:[.,]\\d{3})+|\\d+)");
    private static final List<String> SUPPLIER_PREFIXES = List.of("nha cung cap", "ncc");
    private static final List<String> IGNORED_PREFIXES = List.of("tong", "ngay", "hoa don", "so hd", "thanh toan");

    private final ObjectMapper objectMapper;

    public MockLlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private record CatalogItem(long id, String foldedName, String foldedUnit) {
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        List<CatalogItem> catalog = readCatalog(userPrompt);
        String supplierName = null;
        List<Map<String, Object>> items = new ArrayList<>();
        List<Map<String, Object>> unmatched = new ArrayList<>();
        for (String rawSegment : SEGMENT_SEPARATOR.split(readInvoice(userPrompt))) {
            String segment = rawSegment.trim();
            Optional<String[]> supplierSplit = splitSupplier(segment);
            if (supplierSplit.isPresent()) {
                supplierName = supplierSplit.get()[0];
                segment = supplierSplit.get()[1];
            }
            if (isNoise(segment)) {
                continue;
            }
            readLine(segment, catalog).ifPresentOrElse(items::add, () -> unmatched.add(Map.of(
                    "sourceText", rawSegment.trim(), "reason", "Không có trong danh mục hoặc đơn vị không khớp")));
        }
        return toJson(supplierName, items, unmatched);
    }

    @Override
    public boolean isMock() {
        return true;
    }

    private static List<CatalogItem> readCatalog(String userPrompt) {
        List<CatalogItem> catalog = new ArrayList<>();
        Matcher row = CATALOG_ROW.matcher(userPrompt);
        while (row.find()) {
            catalog.add(new CatalogItem(Long.parseLong(row.group(1)), VietnameseText.fold(row.group(2)),
                    VietnameseText.fold(row.group(3))));
        }
        // Longest name first, so "ca chua bi" wins over "ca chua" when both are in the catalog.
        catalog.sort(Comparator.comparingInt((CatalogItem c) -> c.foldedName().length()).reversed());
        return catalog;
    }

    private static String readInvoice(String userPrompt) {
        Matcher block = INVOICE_BLOCK.matcher(userPrompt);
        return block.find() ? block.group(1) : "";
    }

    /** "NCC Nông sản Xanh - Cà chua 40kg" -> ["NCC Nông sản Xanh" name, "Cà chua 40kg"]. */
    private static Optional<String[]> splitSupplier(String segment) {
        String folded = VietnameseText.fold(segment);
        for (String prefix : SUPPLIER_PREFIXES) {
            if (folded.startsWith(prefix)) {
                String rest = segment.substring(prefix.length()).replaceFirst("^\\s*:?\\s*", "");
                int dash = rest.indexOf(" - ");
                return Optional.of(dash < 0
                        ? new String[] {rest.trim(), ""}
                        : new String[] {rest.substring(0, dash).trim(), rest.substring(dash + 3).trim()});
            }
        }
        return Optional.empty();
    }

    private static boolean isNoise(String segment) {
        String folded = VietnameseText.fold(segment);
        return folded.isEmpty() || folded.chars().noneMatch(Character::isDigit)
                || IGNORED_PREFIXES.stream().anyMatch(folded::startsWith);
    }

    private static Optional<Map<String, Object>> readLine(String segment, List<CatalogItem> catalog) {
        String folded = VietnameseText.fold(segment);
        for (CatalogItem product : catalog) {
            int at = folded.indexOf(product.foldedName());
            if (at < 0) {
                continue;
            }
            String afterName = folded.substring(at + product.foldedName().length());
            Matcher quantity = QUANTITY.matcher(afterName);
            if (!quantity.find() || !unitMatches(quantity.group(2), product.foldedUnit())) {
                return Optional.empty();
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", product.id());
            item.put("quantity", new BigDecimal(quantity.group(1).replace(',', '.')));
            item.put("unitPrice", readPrice(afterName.substring(quantity.end())));
            item.put("sourceText", segment);
            return Optional.of(item);
        }
        return Optional.empty();
    }

    /** No unit written means "the catalog unit"; a different unit cannot be converted safely, so it is rejected. */
    private static boolean unitMatches(String writtenUnit, String catalogUnit) {
        return writtenUnit == null || writtenUnit.equals("x") || writtenUnit.equals(catalogUnit);
    }

    private static BigDecimal readPrice(String text) {
        Matcher price = PRICE.matcher(text);
        return price.find() ? new BigDecimal(price.group(1).replaceAll("[.,]", "")) : null;
    }

    private String toJson(String supplierName, List<Map<String, Object>> items, List<Map<String, Object>> unmatched) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("supplierName", supplierName);
        answer.put("items", items);
        answer.put("unmatched", unmatched);
        try {
            return objectMapper.writeValueAsString(answer);
        } catch (JsonProcessingException e) {
            throw new LlmUnavailableException("Mock LLM could not serialize its answer", e);
        }
    }
}
