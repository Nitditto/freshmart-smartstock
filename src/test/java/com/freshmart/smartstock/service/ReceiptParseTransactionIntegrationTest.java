package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.dto.llm.ParseReceiptRequest;
import com.freshmart.smartstock.dto.llm.ParseReceiptResponse;
import com.freshmart.smartstock.llm.LlmService;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.freshmart.smartstock.support.TestData;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The model call can last minutes, so parse must not hold a database transaction (and its pooled connection)
 * while it waits. The stubbed model records whether a transaction is active at the moment it is called.
 */
class ReceiptParseTransactionIntegrationTest extends AbstractIntegrationTest {

    @MockBean
    private LlmService llmService;
    @Autowired
    private ReceiptParseService parseService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private StockReceiptRepository receiptRepository;
    @Autowired
    private StockIssueRepository issueRepository;

    @Test
    void noTransactionIsActiveWhileTheModelIsCalled() {
        TestData data = new TestData(userRepository, categoryRepository, productRepository, supplierRepository,
                receiptRepository, issueRepository);
        Product product = data.product(0, 0);
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        when(llmService.complete(anyString(), anyString())).thenAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "{\"supplierName\":null,\"items\":[{\"productId\":%d,\"quantity\":3,\"unitPrice\":null,"
                    .formatted(product.getId()) + "\"sourceText\":\"%s 3kg\"}],\"unmatched\":[]}"
                    .formatted(product.getName());
        });

        ParseReceiptResponse response = parseService.parse(new ParseReceiptRequest(product.getName() + " 3kg"));

        assertFalse(transactionActive.get(), "the model was called inside a database transaction");
        assertEquals(1, response.lines().size());
        assertEquals(product.getName(), response.lines().get(0).productName());
    }
}
