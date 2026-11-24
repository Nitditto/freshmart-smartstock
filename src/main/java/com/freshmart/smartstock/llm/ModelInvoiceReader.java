package com.freshmart.smartstock.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmart.smartstock.common.exception.ApiException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns the model's raw answer into a {@link ModelInvoice}. Bad syntax or a wrong shape is the model's fault, not
 * the user's input, but the contract maps it to 400 (api-contract section 5.14) so the client can fall back to
 * manual entry.
 */
@Component
@RequiredArgsConstructor
public class ModelInvoiceReader {

    /** Models often wrap JSON in ```json fences despite being told not to. */
    private static final Pattern CODE_FENCE = Pattern.compile("^```[a-zA-Z]*\\s*(.*?)\\s*```$", Pattern.DOTALL);
    private static final String INVALID_ANSWER = "AI trả về dữ liệu không hợp lệ, vui lòng nhập tay";

    private final ObjectMapper objectMapper;

    public ModelInvoice read(String rawAnswer) {
        String json = stripCodeFence(rawAnswer == null ? "" : rawAnswer.trim());
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject() || !isArrayOrAbsent(root, "items")
                    || !isArrayOrAbsent(root, "unmatched")) {
                throw ApiException.badRequest(INVALID_ANSWER);
            }
            return objectMapper.treeToValue(root, ModelInvoice.class);
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest(INVALID_ANSWER);
        }
    }

    private static String stripCodeFence(String text) {
        Matcher fenced = CODE_FENCE.matcher(text);
        return fenced.matches() ? fenced.group(1) : text;
    }

    private static boolean isArrayOrAbsent(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() || node.isArray();
    }
}
