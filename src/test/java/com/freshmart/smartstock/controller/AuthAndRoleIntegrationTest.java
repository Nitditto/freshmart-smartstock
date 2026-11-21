package com.freshmart.smartstock.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.UserRepository;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Demonstrates the spec's 401 vs 403 behaviour end to end. Extend with each member's endpoints. */
class AuthAndRoleIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Test@12345";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        userRepository.deleteAll();
        userRepository.save(newUser("admin@test.local", Role.ADMIN, true));
        userRepository.save(newUser("staff@test.local", Role.STAFF, true));
        userRepository.save(newUser("locked@test.local", Role.STAFF, false));
    }

    @Test
    void loginSucceedsAndNeverReturnsPasswordHash() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@test.local", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    void meReturnsCurrentUserAndNeverReturnsPasswordHash() throws Exception {
        String token = loginAndGetToken("staff@test.local");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("staff@test.local"))
                .andExpect(jsonPath("$.data.role").value("STAFF"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void wrongPasswordUnknownEmailAndLockedAccountAllReturnTheSame401() throws Exception {
        for (String[] attempt : new String[][]{
                {"admin@test.local", "wrong-password"},
                {"nobody@test.local", PASSWORD},
                {"locked@test.local", PASSWORD}}) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(attempt[0], attempt[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
        }
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCallingAdminEndpointGets403() throws Exception {
        String token = loginAndGetToken("staff@test.local");

        mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Rau củ\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCreatesCategoryThenDuplicateReturns409() throws Exception {
        String token = loginAndGetToken("admin@test.local");
        String body = "{\"name\":\"Đồ khô\"}";

        mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Đồ khô"));

        mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    private User newUser(String email, Role role, boolean active) {
        User u = new User();
        u.setFullName(email);
        u.setEmail(email);
        u.setRole(role);
        u.setActive(active);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return u;
    }

    private String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String loginAndGetToken(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.accessToken");
    }
}
