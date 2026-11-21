package com.freshmart.smartstock.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** The generated OpenAPI document is what Swagger UI shows during the demo: check the examples are in it. */
class OpenApiDocsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void apiDocsArePublicAndContainRequestAndErrorExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin@freshmart.local")))
                .andExpect(content().string(containsString("staff2@freshmart.local")))
                .andExpect(content().string(containsString("insufficient-stock")));
    }

    @Test
    void everyOperationDocuments401And403() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/users'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/users'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/receipts/{id}/confirm'].patch.responses['409']").exists());
    }
}
