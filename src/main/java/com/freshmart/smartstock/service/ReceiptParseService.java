package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.llm.ParseReceiptRequest;
import com.freshmart.smartstock.dto.llm.ParseReceiptResponse;

public interface ReceiptParseService {

    /**
     * Turns free-text invoice into a SUGGESTED receipt. Never saves anything.
     * Invalid model JSON / unknown productId / quantity <= 0 => 400. Provider down => 503.
     */
    ParseReceiptResponse parse(ParseReceiptRequest request);
}
