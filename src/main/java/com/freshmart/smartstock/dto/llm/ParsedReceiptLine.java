package com.freshmart.smartstock.dto.llm;

import java.math.BigDecimal;

public record ParsedReceiptLine(Long productId, String productName, Integer quantity, BigDecimal unitPrice,
                                String sourceText) {
}
