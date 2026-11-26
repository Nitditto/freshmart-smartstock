package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.dto.llm.ParseReceiptRequest;
import com.freshmart.smartstock.dto.llm.ParseReceiptResponse;
import com.freshmart.smartstock.dto.llm.ParsedReceiptLine;
import com.freshmart.smartstock.dto.llm.UnmatchedLine;
import com.freshmart.smartstock.llm.LlmService;
import com.freshmart.smartstock.llm.LlmUnavailableException;
import com.freshmart.smartstock.llm.ModelInvoice;
import com.freshmart.smartstock.llm.ModelInvoiceReader;
import com.freshmart.smartstock.llm.PromptTemplates;
import com.freshmart.smartstock.llm.VietnameseText;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.service.ReceiptParseService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Invoice text -> SUGGESTED receipt. Pipeline: catalog -> prompt -> LLM -> JSON -> business validation -> supplier
 * match. Nothing is persisted. The model is treated as untrusted: even if a prompt injection fools it, step 5 only
 * lets through active products that exist in our own catalog.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptParseServiceImpl implements ReceiptParseService {

    static final String REASON_NOT_MATCHED = "AI không khớp được sản phẩm trong danh mục";
    static final String REASON_UNKNOWN_PRODUCT = "Sản phẩm không tồn tại hoặc đã ẩn";
    static final String REASON_BAD_QUANTITY = "Số lượng phải là số nguyên > 0";
    static final String REASON_NEGATIVE_PRICE = "Đơn giá không được âm";
    static final String REASON_DUPLICATE = "Sản phẩm đã có ở dòng trước";
    static final String REASON_NOT_IN_INVOICE = "Dòng AI trả về không có trong hóa đơn";
    static final String REASON_NAME_MISMATCH = "Tên sản phẩm không xuất hiện trong dòng hóa đơn";
    static final String REASON_QUANTITY_NOT_IN_LINE = "Số lượng không khớp với dòng hóa đơn";
    static final String REASON_UNIT_MISMATCH = "Đơn vị trong hóa đơn khác đơn vị của sản phẩm";
    static final String REASON_PRICE_NOT_IN_LINE = "Đơn giá không khớp với dòng hóa đơn";
    private static final Pattern THOUSANDS_SEPARATOR = Pattern.compile("(?<=\\d)[.,](?=\\d{3}(?!\\d))");

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final LlmService llmService;
    private final PromptTemplates prompts;
    private final ModelInvoiceReader modelInvoiceReader;

    /**
     * Deliberately NOT @Transactional: the model call can take minutes (local Ollama), and a surrounding transaction
     * would keep a pooled DB connection checked out the whole time. Each repository read below opens and releases
     * its own connection; the products are fetched with their category, so nothing needs a session afterwards.
     */
    @Override
    public ParseReceiptResponse parse(ParseReceiptRequest request) {
        Map<Long, Product> catalog = productRepository.search(null, null, true, Pageable.unpaged(Sort.by("id")))
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        ModelInvoice invoice = modelInvoiceReader.read(askModel(catalog.values(), request.text()));
        String foldedInvoice = VietnameseText.fold(request.text());

        List<ParsedReceiptLine> lines = new ArrayList<>();
        List<UnmatchedLine> unmatched = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ModelInvoice.Item item : invoice.itemsOrEmpty()) {
            Optional<String> rejection = rejectionReason(item, catalog, foldedInvoice, seen);
            if (rejection.isPresent()) {
                unmatched.add(new UnmatchedLine(item.sourceText(), rejection.get()));
            } else {
                lines.add(toLine(item, catalog.get(item.productId())));
            }
        }
        invoice.unmatchedOrEmpty().forEach(u -> unmatched.add(new UnmatchedLine(u.sourceText(),
                u.reason() == null ? REASON_NOT_MATCHED : u.reason())));

        log.info("Invoice parsed: {} lines, {} unmatched, mock={}", lines.size(), unmatched.size(),
                llmService.isMock());
        return new ParseReceiptResponse(matchSupplier(invoice.supplierName()), invoice.supplierName(),
                lines, unmatched, llmService.isMock());
    }

    private String askModel(Iterable<Product> catalog, String invoiceText) {
        StringBuilder catalogText = new StringBuilder();
        // Only id/sku/name/unit: prices and user data never leave the system (AGENTS.md section 6).
        catalog.forEach(p -> catalogText.append("%d | %s | %s | %s%n"
                .formatted(p.getId(), p.getSku(), p.getName(), p.getUnit())));
        String user = prompts.render(prompts.receiptParseUserPath(), Map.of(
                "PRODUCT_CATALOG", catalogText.toString().trim(),
                "INVOICE_TEXT", neutralizeDelimiters(invoiceText.trim())));
        try {
            return llmService.complete(prompts.load(prompts.receiptParseSystemPath()), user);
        } catch (LlmUnavailableException e) {
            log.warn("LLM unavailable: {}", e.getMessage());
            throw ApiException.serviceUnavailable("Dịch vụ AI tạm thời không khả dụng, vui lòng nhập tay");
        }
    }

    /** The invoice sits between <<< and >>> in the prompt; an invoice containing them could fake the end of data. */
    private static String neutralizeDelimiters(String text) {
        return text.replace("<<<", "< < <").replace(">>>", "> > >");
    }

    private static Optional<String> rejectionReason(ModelInvoice.Item item, Map<Long, Product> catalog,
                                                    String foldedInvoice, Set<Long> seen) {
        if (item.productId() == null) {
            return Optional.of(REASON_NOT_MATCHED);
        }
        Product product = catalog.get(item.productId());
        if (product == null) {
            return Optional.of(REASON_UNKNOWN_PRODUCT);
        }
        if (!isPositiveInteger(item.quantity())) {
            return Optional.of(REASON_BAD_QUANTITY);
        }
        if (item.unitPrice() != null && item.unitPrice().signum() < 0) {
            return Optional.of(REASON_NEGATIVE_PRICE);
        }
        Optional<String> mismatch = mismatchWithInvoiceLine(item, product, foldedInvoice);
        if (mismatch.isPresent()) {
            return mismatch;
        }
        return seen.add(item.productId()) ? Optional.empty() : Optional.of(REASON_DUPLICATE);
    }

    /**
     * A real but WRONG answer (product "Gạo" for a "Sữa tươi" line, 26 for "26.000", 12 for "12.5kg") passes the
     * catalog checks, so every value must be found in the very invoice line the model quotes. Trade-off: a synonym
     * that is not the catalog name ends up in unmatched and is typed by hand.
     */
    private static Optional<String> mismatchWithInvoiceLine(ModelInvoice.Item item, Product product,
                                                            String foldedInvoice) {
        String line = VietnameseText.fold(item.sourceText());
        if (line.isEmpty() || !foldedInvoice.contains(line)) {
            return Optional.of(REASON_NOT_IN_INVOICE);
        }
        if (!line.contains(VietnameseText.fold(product.getName()))
                && !line.contains(VietnameseText.fold(product.getSku()))) {
            return Optional.of(REASON_NAME_MISMATCH);
        }
        Optional<String> quantityProblem = quantityProblem(line, item.quantity().toBigInteger().toString(),
                VietnameseText.fold(product.getUnit()));
        if (quantityProblem.isPresent()) {
            return quantityProblem;
        }
        return item.unitPrice() == null || containsNumber(withoutThousandsSeparators(line), item.unitPrice())
                ? Optional.empty() : Optional.of(REASON_PRICE_NOT_IN_LINE);
    }

    /** The quantity must be written as a whole number ("12" is not in "12.5kg"), followed by no unit or ours. */
    private static Optional<String> quantityProblem(String line, String quantity, String catalogUnit) {
        Matcher written = Pattern.compile("(?<![\\d.,])" + quantity + "(?![.,]?\\d)\\s*(\\p{L}+)?").matcher(line);
        boolean found = false;
        while (written.find()) {
            found = true;
            String unit = written.group(1);
            if (unit == null || unit.equals("x") || unit.equals(catalogUnit)) {
                return Optional.empty();
            }
        }
        return Optional.of(found ? REASON_UNIT_MISMATCH : REASON_QUANTITY_NOT_IN_LINE);
    }

    /** Vietnamese prices use "." or "," as thousands separators: "1.250.000" -> "1250000"; "12.5" stays. */
    private static String withoutThousandsSeparators(String line) {
        return THOUSANDS_SEPARATOR.matcher(line).replaceAll("");
    }

    private static boolean containsNumber(String line, BigDecimal number) {
        String plain = Pattern.quote(number.stripTrailingZeros().toPlainString());
        return Pattern.compile("(?<![\\d.,])" + plain + "(?![.,]?\\d)").matcher(line).find();
    }

    private static boolean isPositiveInteger(BigDecimal quantity) {
        return quantity != null && quantity.signum() > 0 && quantity.stripTrailingZeros().scale() <= 0
                && quantity.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0;
    }

    private static ParsedReceiptLine toLine(ModelInvoice.Item item, Product product) {
        return new ParsedReceiptLine(product.getId(), product.getName(), item.quantity().intValueExact(),
                item.unitPrice(), item.sourceText());
    }

    /** Suggest a supplier only when exactly one name matches, so the suggestion is never a coin toss. */
    private Long matchSupplier(String supplierNameInText) {
        String wanted = VietnameseText.fold(supplierNameInText);
        if (wanted.isEmpty()) {
            return null;
        }
        List<Supplier> candidates = supplierRepository.findAll().stream()
                .filter(s -> {
                    String name = VietnameseText.fold(s.getName());
                    return name.contains(wanted) || wanted.contains(name);
                })
                .toList();
        return candidates.size() == 1 ? candidates.get(0).getId() : null;
    }
}
