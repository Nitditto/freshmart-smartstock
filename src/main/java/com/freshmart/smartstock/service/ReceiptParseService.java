package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.llm.ParseReceiptRequest;
import com.freshmart.smartstock.dto.llm.ParseReceiptResponse;

public interface ReceiptParseService {

    /**
     * Turns free-text invoice into a SUGGESTED receipt. Never saves anything.
     * Model JSON with bad syntax/shape => 400. Unknown or hidden productId, quantity not a positive integer,
     * negative price or a repeated product => that line goes to {@code unmatched}. Provider down => 503.
     */
    ParseReceiptResponse parse(ParseReceiptRequest request);
}
