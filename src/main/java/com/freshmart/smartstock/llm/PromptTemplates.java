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

    public static final String RECEIPT_PARSE_SYSTEM = "prompts/receipt-parse-v1.system.txt";
    public static final String RECEIPT_PARSE_USER = "prompts/receipt-parse-v1.user.txt";

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
