package com.freshmart.smartstock.dto.issue;

import com.freshmart.smartstock.dto.inventory.LowStockItem;
import java.util.List;

/**
 * Result of confirming an issue: the confirmed document plus the products that this confirmation
 * pushed down to/below MinStock (spec: "kết quả xác nhận phiếu xuất bao gồm các sản phẩm vừa giảm xuống dưới mức tối thiểu").
 */
public record IssueConfirmResponse(IssueResponse issue, List<LowStockItem> newlyLowStock) {
}
