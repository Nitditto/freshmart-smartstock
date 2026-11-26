package com.freshmart.smartstock.dto.inventory;

import com.freshmart.smartstock.domain.enums.MovementDirection;
import com.freshmart.smartstock.domain.enums.MovementDocumentType;
import java.time.Instant;

/** One stock movement of a product, produced by a CONFIRMED document. */
public record ProductHistoryItem(
        Instant occurredAt,
        MovementDirection direction,
        MovementDocumentType documentType,
        String documentCode,
        int quantity) {
}
