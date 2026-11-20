package com.freshmart.smartstock.dto.inventory;

/** Returned in the `data` of a 409 when confirming an issue that exceeds stock. Nothing is deducted. */
public record ShortageItem(Long productId, String sku, String name, int requested, int available) {
}
