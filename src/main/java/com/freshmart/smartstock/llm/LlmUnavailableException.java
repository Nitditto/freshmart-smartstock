package com.freshmart.smartstock.llm;

/** Provider timeout / network / 5xx. The parse service maps this to a clean API error (503), not a stack trace. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
