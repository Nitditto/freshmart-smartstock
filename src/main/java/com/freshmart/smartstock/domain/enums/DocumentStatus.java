package com.freshmart.smartstock.domain.enums;

/**
 * Lifecycle of a receipt / issue.
 * Allowed: DRAFT -> CONFIRMED, DRAFT -> CANCELLED. CONFIRMED and CANCELLED are terminal (changes => 409).
 */
public enum DocumentStatus {
    DRAFT,
    CONFIRMED,
    CANCELLED;

    public boolean isTerminal() {
        return this != DRAFT;
    }
}
