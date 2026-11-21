package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.config.AppProperties;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.security.AuthUser;
import com.freshmart.smartstock.security.JwtService;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * 401/403 matrix from docs/api-contract.md section 3, every endpoint x every role.
 * Bodies are VALID on purpose: Bean Validation runs before @PreAuthorize, so an invalid body would give 400 and
 * hide a missing role check. Allowed roles are only asserted as "not 401/403" until all owners have merged
 * (tighten on 27/11).
 */
class SecurityMatrixIntegrationTest extends AbstractIntegrationTest {

    private static final String PRODUCT = "{\"sku\":\"S-1\",\"name\":\"N\",\"categoryId\":1,\"unit\":\"kg\","
            + "\"price\":1,\"minStock\":0}";
    private static final String SUPPLIER = "{\"name\":\"NCC\"}";
    private static final String RECEIPT = "{\"supplierId\":1,\"lines\":[{\"productId\":1,\"quantity\":1,"
            + "\"unitPrice\":1}]}";
    private static final String ISSUE = "{\"reason\":\"SALE\",\"lines\":[{\"productId\":1,\"quantity\":1}]}";
    private static final String USER = "{\"fullName\":\"A\",\"email\":\"matrix@x.com\",\"password\":\"Secret@123\","
            + "\"role\":\"STAFF\"}";
    private static final String STATUS = "{\"active\":true}";
    private static final String PARSE = "{\"text\":\"x\"}";

    private static final long STAFF_ID = 900_001L;
    private static final long ADMIN_ID = 900_002L;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private AppProperties properties;
    @Value("${app.jwt.secret}")
    private String secret;

    /** (method, path, body, adminOnly). */
    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/users", null, true),
                Arguments.of(HttpMethod.POST, "/api/users", USER, true),
                Arguments.of(HttpMethod.PATCH, "/api/users/1/status", STATUS, true),
                Arguments.of(HttpMethod.GET, "/api/categories", null, false),
                Arguments.of(HttpMethod.POST, "/api/categories", "{\"name\":\"C\"}", true),
                Arguments.of(HttpMethod.GET, "/api/products", null, false),
                Arguments.of(HttpMethod.GET, "/api/products/1", null, false),
                Arguments.of(HttpMethod.POST, "/api/products", PRODUCT, true),
                Arguments.of(HttpMethod.PUT, "/api/products/1", PRODUCT, true),
                Arguments.of(HttpMethod.DELETE, "/api/products/1", null, true),
                Arguments.of(HttpMethod.GET, "/api/suppliers", null, false),
                Arguments.of(HttpMethod.POST, "/api/suppliers", SUPPLIER, true),
                Arguments.of(HttpMethod.PUT, "/api/suppliers/1", SUPPLIER, true),
                Arguments.of(HttpMethod.GET, "/api/receipts", null, false),
                Arguments.of(HttpMethod.GET, "/api/receipts/1", null, false),
                Arguments.of(HttpMethod.POST, "/api/receipts", RECEIPT, false),
                Arguments.of(HttpMethod.POST, "/api/receipts/parse", PARSE, false),
                Arguments.of(HttpMethod.GET, "/api/issues", null, false),
                Arguments.of(HttpMethod.GET, "/api/issues/1", null, false),
                Arguments.of(HttpMethod.POST, "/api/issues", ISSUE, false),
                Arguments.of(HttpMethod.GET, "/api/inventory/low-stock", null, false),
                Arguments.of(HttpMethod.GET, "/api/products/1/history", null, false),
                Arguments.of(HttpMethod.GET, "/api/reports/stock-summary?from=2026-11-01&to=2026-11-30", null, true));
    }

    @ParameterizedTest(name = "{0} {1} without token -> 401")
    @MethodSource("endpoints")
    void withoutTokenReturns401(HttpMethod method, String path, String body, boolean adminOnly) throws Exception {
        mockMvc.perform(call(method, path, body, null))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest(name = "{0} {1} as STAFF")
    @MethodSource("endpoints")
    void staffGets403OnlyOnAdminEndpoints(HttpMethod method, String path, String body, boolean adminOnly)
            throws Exception {
        String token = bearer(jwtService, STAFF_ID, Role.STAFF);
        if (adminOnly) {
            mockMvc.perform(call(method, path, body, token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        } else {
            assertNotAuthError(method, path, body, token);
        }
    }

    @ParameterizedTest(name = "{0} {1} as ADMIN")
    @MethodSource("endpoints")
    void adminIsNeverRejectedByTheRoleCheck(HttpMethod method, String path, String body, boolean adminOnly)
            throws Exception {
        assertNotAuthError(method, path, body, bearer(jwtService, ADMIN_ID, Role.ADMIN));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(call(HttpMethod.GET, "/api/auth/me", null, null)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        Instant longAgo = Instant.parse("2020-01-01T00:00:00Z");
        JwtService past = new JwtService(propsWithSecret(secret), Clock.fixed(longAgo, ZoneOffset.UTC));

        mockMvc.perform(call(HttpMethod.GET, "/api/auth/me", null, bearer(past, STAFF_ID, Role.STAFF)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherSecretReturns401() throws Exception {
        JwtService forged = new JwtService(propsWithSecret("another-secret-another-secret-another-0123"),
                Clock.systemUTC());

        mockMvc.perform(call(HttpMethod.GET, "/api/auth/me", null, bearer(forged, ADMIN_ID, Role.ADMIN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenReturns401WithJsonEnvelope() throws Exception {
        mockMvc.perform(call(HttpMethod.GET, "/api/users", null, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private void assertNotAuthError(HttpMethod method, String path, String body, String token) throws Exception {
        int code = mockMvc.perform(call(method, path, body, token)).andReturn().getResponse().getStatus();
        assertNotEquals(401, code, method + " " + path);
        assertNotEquals(403, code, method + " " + path);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder call(
            HttpMethod method, String path, String body, String token) {
        var builder = request(method, path);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder;
    }

    private static String bearer(JwtService service, long id, Role role) {
        return "Bearer " + service.generateToken(new AuthUser(id, "u" + id + "@test.local", role));
    }

    private AppProperties propsWithSecret(String jwtSecret) {
        return new AppProperties(properties.timezone(), new AppProperties.Jwt(jwtSecret, 60),
                properties.llm(), properties.seed());
    }
}
