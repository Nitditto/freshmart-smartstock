package com.freshmart.smartstock.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * The "Authorize" button sends: Authorization: Bearer {token}
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI smartStockOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartStock API")
                        .version("v1")
                        .description("Warehouse management REST API for FreshMart. "
                                + "Login via POST /api/auth/login, then click Authorize and paste the accessToken."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /** Error responses every endpoint can produce, documented once instead of on each controller method. */
    private static final String ISSUE_CONTROLLER = "IssueController";

    private static final Map<String, String> ERROR_RESPONSES = Map.of(
            "400", "Dữ liệu không hợp lệ",
            "401", "Chưa xác thực hoặc token không hợp lệ",
            "403", "Không đủ quyền",
            "404", "Không tìm thấy",
            "409", "Xung đột nghiệp vụ / trạng thái (ví dụ thiếu tồn kho)");

    @Bean
    public OperationCustomizer standardErrorResponses() {
        return (operation, handlerMethod) -> {
            if (isIssueConfirm(handlerMethod)) {
                addShortageExample(operation);
            }
            return addMissingErrorResponses(operation);
        };
    }

    private static boolean isIssueConfirm(HandlerMethod handlerMethod) {
        return ISSUE_CONTROLLER.equals(handlerMethod.getBeanType().getSimpleName())
                && "confirm".equals(handlerMethod.getMethod().getName());
    }

    /** Demo-critical 409: confirming an issue larger than stock lists the shortages and deducts nothing. */
    static void addShortageExample(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        Map<String, Object> shortage = new LinkedHashMap<>();
        shortage.put("productId", 3);
        shortage.put("sku", "SP-003");
        shortage.put("name", "Sữa tươi 1L");
        shortage.put("requested", 50);
        shortage.put("available", 12);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("message", "Không đủ tồn kho cho một số sản phẩm, không có gì bị trừ");
        body.put("data", List.of(shortage));
        MediaType json = new MediaType().schema(new ObjectSchema())
                .addExamples("insufficient-stock", new Example().value(body));
        operation.getResponses().addApiResponse("409", new ApiResponse()
                .description("Thiếu tồn kho: data là danh sách ShortageItem, tồn kho không đổi")
                .content(new Content().addMediaType("application/json", json)));
    }

    static Operation addMissingErrorResponses(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        ERROR_RESPONSES.forEach((code, description) ->
                operation.getResponses().computeIfAbsent(code, c -> errorResponse(c, description)));
        return operation;
    }

    private static ApiResponse errorResponse(String code, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", Integer.parseInt(code));
        body.put("message", description);
        body.put("data", "400".equals(code) ? Map.of("email", "Email không hợp lệ") : null);
        Example example = new Example().value(body);
        MediaType json = new MediaType().schema(new ObjectSchema()).addExamples("error", example);
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", json));
    }
}
