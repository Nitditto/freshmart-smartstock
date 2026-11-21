package com.freshmart.smartstock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void addsAllStandardErrorResponsesToAnOperation() {
        Operation operation = OpenApiConfig.addMissingErrorResponses(new Operation());

        assertTrue(operation.getResponses().keySet().containsAll(List.of("400", "401", "403", "404", "409")));
    }

    @Test
    void keepsResponsesAlreadyDocumentedOnTheEndpoint() {
        ApiResponse custom = new ApiResponse().description("custom 409");
        Operation operation = new Operation().responses(new ApiResponses().addApiResponse("409", custom));

        OpenApiConfig.addMissingErrorResponses(operation);

        assertSame(custom, operation.getResponses().get("409"));
        assertEquals(5, operation.getResponses().size());
    }

    @Test
    void shortageExampleListsProductsAndReplacesTheGeneric409() {
        Operation operation = new Operation();

        OpenApiConfig.addShortageExample(operation);
        OpenApiConfig.addMissingErrorResponses(operation);

        assertTrue(operation.getResponses().get("409").getContent().get("application/json").getExamples()
                .containsKey("insufficient-stock"));
    }
}
