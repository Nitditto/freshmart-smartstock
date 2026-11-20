package com.freshmart.smartstock.dto.inventory;

import java.time.Instant;

/** One stock movement of a product, produced by a CONFIRMED document. direction: IN (receipt) | OUT (issue). */
public record ProductHistoryItem(
        Instant occurredAt,
        String direction,
        String documentType,
        String documentCode,
        int quantity) {
}
