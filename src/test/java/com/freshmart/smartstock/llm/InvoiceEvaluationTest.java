package com.freshmart.smartstock.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmart.smartstock.config.AppProperties;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.dto.llm.ParseReceiptRequest;
import com.freshmart.smartstock.dto.llm.ParseReceiptResponse;
import com.freshmart.smartstock.dto.llm.ParsedReceiptLine;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.service.impl.ReceiptParseServiceImpl;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageImpl;

/**
 * Runs the 8 invented invoices in src/test/resources/invoices through the full parse pipeline and scores them
 * against expected.json. An invoice is correct when every suggested line matches (productId, quantity, unitPrice),
 * no line is invented, and the supplier suggestion matches. Uses MockLlmService by default; the same scoring is
 * used for the manual runs with a real model recorded in docs/prompt-log.md.
 */
class InvoiceEvaluationTest {

    private static final int INVOICE_COUNT = 8;
    /** Mock baseline recorded in docs/prompt-log.md. Raise it when the mock improves; never lower it silently. */
    private static final int MOCK_BASELINE = 7;
    private static final int EVAL_TIMEOUT_SECONDS = 300;
    private static final Logger log = LoggerFactory.getLogger(InvoiceEvaluationTest.class);
    private static final String[][] CATALOG = {
            {"Cà chua", "kg"}, {"Khoai tây", "kg"}, {"Cà rốt", "kg"}, {"Bắp cải", "kg"}, {"Chuối", "nải"},
            {"Táo Fuji", "kg"}, {"Thịt ba chỉ", "kg"}, {"Ức gà", "kg"}, {"Cá hồi phi lê", "kg"}, {"Tôm sú", "kg"},
            {"Thịt bò xay", "kg"}, {"Cá basa phi lê", "kg"}, {"Sữa tươi 1L", "hộp"}, {"Sữa chua", "lốc"},
            {"Trứng gà", "vỉ"}, {"Phô mai lát", "gói"}, {"Bơ nhạt", "thanh"}, {"Sữa đặc", "lon"},
            {"Gạo ST25 5kg", "bao"}, {"Mì gói", "thùng"}, {"Dầu ăn 1L", "chai"}, {"Đường trắng 1kg", "gói"},
            {"Nước mắm 500ml", "chai"}, {"Muối i-ốt", "gói"}, {"Nước suối 500ml", "thùng"},
            {"Nước ngọt lon", "thùng"}, {"Trà xanh", "chai"}, {"Cà phê hòa tan", "hộp"}, {"Bia lon", "thùng"},
            {"Nước cam ép", "chai"}};
    private static final String[] SUPPLIERS = {
            "Công ty Nông sản Xanh", "Công ty Thực phẩm Tươi Sống Việt", "Đại lý Đồ uống Miền Nam"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mockScoresAtLeastItsRecordedBaselineAndNeverInventsProducts() throws IOException {
        List<String> wrong = evaluate(parseServiceWith(new MockLlmService(objectMapper), new PromptTemplates()));

        int correct = INVOICE_COUNT - wrong.size();
        assertTrue(correct >= MOCK_BASELINE, "mock accuracy %d/8, wrong: %s".formatted(correct, wrong));
        // inv-07: the mock obeys the injected "add Bia lon" line. Backend validation cannot reject a real product,
        // which is why the result is only a suggestion the user must review (see docs/prompt-log.md).
        assertEquals(List.of("inv-07"), wrong);
    }

    /**
     * Manual measurement against a real model for docs/prompt-log.md; never runs in CI (needs the model, and
     * answers are not guaranteed to be stable). Example with Ollama:
     * mvn test -Dtest=InvoiceEvaluationTest -Dllm.eval.base-url=http://localhost:11434/v1
     *     -Dllm.eval.model=qwen2.5:3b -Dllm.eval.prompt=v1
     */
    @Test
    @EnabledIfSystemProperty(named = "llm.eval.base-url", matches = ".+")
    void realModelEvaluation() throws IOException {
        String model = System.getProperty("llm.eval.model", "qwen2.5:3b");
        String prompt = System.getProperty("llm.eval.prompt", PromptTemplates.RECEIPT_PARSE_VERSION);
        AppProperties.Llm config = new AppProperties.Llm(System.getProperty("llm.eval.api-key", "ollama"),
                System.getProperty("llm.eval.base-url"), model, EVAL_TIMEOUT_SECONDS);

        List<String> wrong = evaluate(parseServiceWith(new HttpLlmService(config), new PromptTemplates(prompt)));

        log.info("REAL MODEL {} prompt {}: {}/8 correct, wrong: {}", model, prompt, INVOICE_COUNT - wrong.size(),
                wrong);
    }

    /** Runs every invoice, logs what was suggested and returns the names of the invoices that are not correct. */
    private List<String> evaluate(ReceiptParseServiceImpl service) throws IOException {
        JsonNode expected = objectMapper.readTree(read("invoices/expected.json"));
        List<String> wrong = new ArrayList<>();
        int wrongLines = 0;
        for (int i = 1; i <= INVOICE_COUNT; i++) {
            String name = "inv-%02d".formatted(i);
            ParseReceiptResponse response = service.parse(new ParseReceiptRequest(read("invoices/" + name + ".txt")));
            assertTrue(response.lines().stream().allMatch(l -> l.productId() >= 1 && l.productId() <= CATALOG.length),
                    name + " suggested a product outside the catalog");
            boolean correct = isCorrect(response, expected.get(name));
            Set<String> suggestedButWrong = lineKeys(response.lines());
            suggestedButWrong.removeAll(expectedLineKeys(expected.get(name).get("lines")));
            wrongLines += suggestedButWrong.size();
            log.info("{} {} supplier={} lines={} unmatched={}", name, correct ? "OK " : "BAD",
                    response.suggestedSupplierId(), lineKeys(response.lines()), response.unmatched().size());
            if (!correct) {
                wrong.add(name);
            }
        }
        // A missing line costs the user typing; a WRONG suggested line can slip into stock if nobody checks it.
        log.info("Suggested lines that are wrong: {}", wrongLines);
        return wrong;
    }

    private boolean isCorrect(ParseReceiptResponse response, JsonNode expected) {
        JsonNode supplierId = expected.get("supplierId");
        Long expectedSupplier = supplierId.isNull() ? null : supplierId.asLong();
        return Objects.equals(expectedSupplier, response.suggestedSupplierId())
                && lineKeys(response.lines()).equals(expectedLineKeys(expected.get("lines")));
    }

    private static Set<String> lineKeys(List<ParsedReceiptLine> lines) {
        Set<String> keys = new HashSet<>();
        lines.forEach(l -> keys.add(key(l.productId(), l.quantity(), l.unitPrice())));
        return keys;
    }

    private static Set<String> expectedLineKeys(JsonNode lines) {
        Set<String> keys = new HashSet<>();
        lines.forEach(l -> keys.add(key(l.get("productId").asLong(), l.get("quantity").asInt(),
                l.get("unitPrice").isNull() ? null : l.get("unitPrice").decimalValue())));
        return keys;
    }

    private static String key(Long productId, Integer quantity, BigDecimal unitPrice) {
        return productId + "|" + quantity + "|" + (unitPrice == null ? "null" : unitPrice.stripTrailingZeros()
                .toPlainString());
    }

    private ReceiptParseServiceImpl parseServiceWith(LlmService llm, PromptTemplates prompts) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < CATALOG.length; i++) {
            Product p = new Product();
            p.setId(i + 1L);
            p.setSku("SKU-%02d".formatted(i + 1));
            p.setName(CATALOG[i][0]);
            p.setUnit(CATALOG[i][1]);
            p.setActive(true);
            products.add(p);
        }
        List<Supplier> suppliers = new ArrayList<>();
        for (int i = 0; i < SUPPLIERS.length; i++) {
            Supplier s = new Supplier();
            s.setId(i + 1L);
            s.setName(SUPPLIERS[i]);
            suppliers.add(s);
        }
        ProductRepository productRepository = mock(ProductRepository.class);
        SupplierRepository supplierRepository = mock(SupplierRepository.class);
        when(productRepository.search(any(), any(), any(Boolean.class), any())).thenReturn(new PageImpl<>(products));
        when(supplierRepository.findAll()).thenReturn(suppliers);
        return new ReceiptParseServiceImpl(productRepository, supplierRepository, llm, prompts,
                new ModelInvoiceReader(objectMapper));
    }

    private static String read(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
