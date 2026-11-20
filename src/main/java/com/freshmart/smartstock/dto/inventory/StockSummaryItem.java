package com.freshmart.smartstock.dto.inventory;

/** Movement of one product between two dates: opening + totalIn - totalOut = closing. */
public record StockSummaryItem(
        Long productId,
        String sku,
        String name,
        int openingQty,
        int totalIn,
        int totalOut,
        int closingQty) {
}
