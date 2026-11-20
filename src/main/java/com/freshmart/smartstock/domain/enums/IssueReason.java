package com.freshmart.smartstock.domain.enums;

public enum IssueReason {
    SALE,
    /** Only ADMIN may confirm an issue with this reason. */
    DAMAGED,
    INTERNAL
}
