package com.freshmart.smartstock.dto.receipt;

import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import java.math.BigDecimal;

public record ReceiptLineResponse(Long productId, String sku, String productName, int quantity, BigDecimal unitPrice) {

    public static ReceiptLineResponse from(StockReceiptDetail d) {
        return new ReceiptLineResponse(d.getProduct().getId(), d.getProduct().getSku(),
                d.getProduct().getName(), d.getQuantity(), d.getUnitPrice());
    }
}
