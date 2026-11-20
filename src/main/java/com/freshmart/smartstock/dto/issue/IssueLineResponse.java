package com.freshmart.smartstock.dto.issue;

import com.freshmart.smartstock.domain.entity.StockIssueDetail;

public record IssueLineResponse(Long productId, String sku, String productName, int quantity) {

    public static IssueLineResponse from(StockIssueDetail d) {
        return new IssueLineResponse(d.getProduct().getId(), d.getProduct().getSku(),
                d.getProduct().getName(), d.getQuantity());
    }
}
