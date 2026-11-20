package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.dto.llm.ParseReceiptRequest;
import com.freshmart.smartstock.dto.llm.ParseReceiptResponse;
import com.freshmart.smartstock.service.ReceiptParseService;
import org.springframework.stereotype.Service;

/**
 * TODO[Member E]. Pipeline:
 * 1) load active products -> "id | sku | name | unit" catalog text (no prices, no customer data)
 * 2) PromptTemplates.render(RECEIPT_PARSE_USER, {PRODUCT_CATALOG, INVOICE_TEXT}); system prompt from file
 * 3) LlmService.complete(...)  (catch LlmUnavailableException -> ApiException 503 "AI tạm thời không khả dụng")
 * 4) parse JSON with ObjectMapper; invalid JSON -> 400
 * 5) validate: productId exists & active, quantity > 0 (drop to `unmatched` or 400 - decide + document)
 * 6) map to ParseReceiptResponse(mock = llmService.isMock()); DO NOT persist anything
 */
@Service
public class ReceiptParseServiceImpl implements ReceiptParseService {

    @Override
    public ParseReceiptResponse parse(ParseReceiptRequest request) {
        throw ApiException.notImplemented("Member E");
    }
}
