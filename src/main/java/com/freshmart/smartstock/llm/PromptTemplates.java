package com.freshmart.smartstock.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Loads prompt text files from classpath:prompts/ and fills {{PLACEHOLDERS}}.
 * Prompts live in files (not Java strings) so every version can be diffed in Git and quoted in docs/prompt-log.md.
 */
@Component
public class PromptTemplates {

    /** Prompt version used by the running app; older versions stay in the repo for the before/after comparison. */
    public static final String RECEIPT_PARSE_VERSION = "v2";

    private final String receiptParseVersion;

    public PromptTemplates() {
        this(RECEIPT_PARSE_VERSION);
    }

    /** Lets the invoice evaluation run any prompt version against the same pipeline. */
    public PromptTemplates(String receiptParseVersion) {
        this.receiptParseVersion = receiptParseVersion;
    }

    public String receiptParseSystemPath() {
        return "prompts/receipt-parse-%s.system.txt".formatted(receiptParseVersion);
    }

    public String receiptParseUserPath() {
        return "prompts/receipt-parse-%s.user.txt".formatted(receiptParseVersion);
    }

    public String load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read prompt " + path, e);
        }
    }

    public String render(String path, Map<String, String> values) {
        String text = load(path);
        for (Map.Entry<String, String> e : values.entrySet()) {
            text = text.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return text;
    }
}
