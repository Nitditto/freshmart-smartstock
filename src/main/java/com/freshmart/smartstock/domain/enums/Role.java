package com.freshmart.smartstock.domain.enums;

public enum Role {
    /** Manages products, categories, suppliers, user accounts; views reports; may confirm DAMAGED issues. */
    ADMIN,
    /** Warehouse staff: views data, creates documents; edits/confirms/cancels only their own documents. */
    STAFF
}
