package com.freshmart.smartstock.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

/**
 * The JSON the model is asked to return (schema in prompts/receipt-parse-*.system.txt). Values are UNTRUSTED:
 * extra fields are ignored, and quantity is a BigDecimal so a fractional answer ("2.5") reaches validation
 * instead of failing deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelInvoice(String supplierName, List<Item> items, List<Unmatched> unmatched) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long productId, BigDecimal quantity, BigDecimal unitPrice, String sourceText) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Unmatched(String sourceText, String reason) {
    }

    public List<Item> itemsOrEmpty() {
        return items == null ? List.of() : items;
    }

    public List<Unmatched> unmatchedOrEmpty() {
        return unmatched == null ? List.of() : unmatched;
    }
}
