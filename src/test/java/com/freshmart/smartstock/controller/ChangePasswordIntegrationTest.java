package com.freshmart.smartstock.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/** PATCH /api/auth/password end to end: the new password works for login, the old one does not. */
class ChangePasswordIntegrationTest extends AbstractIntegrationTest {

    private static final String OLD_PASSWORD = "OldPass@123";
    private static final String NEW_PASSWORD = "NewPass@456";
    private static final String PASSWORD_URL = "/api/auth/password";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email;

    @BeforeEach
    void createUser() {
        email = "pwd" + System.nanoTime() + "@freshmart.test";
        User user = new User();
        user.setFullName("Password Tester");
        user.setEmail(email);
        user.setRole(Role.STAFF);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
        userRepository.save(user);
    }

    @Test
    void changePasswordLetsUserLoginWithNewPasswordOnly() throws Exception {
        String token = loginAndGetToken(OLD_PASSWORD);

        mockMvc.perform(patch(PASSWORD_URL).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(OLD_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(OLD_PASSWORD)))
                .andExpect(status().isUnauthorized());
        assertTrue(loginAndGetToken(NEW_PASSWORD).startsWith("Bearer "));
        assertTrue(passwordEncoder.matches(NEW_PASSWORD,
                userRepository.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash()));
    }

    @Test
    void wrongCurrentPasswordReturns400AndKeepsOldPassword() throws Exception {
        String token = loginAndGetToken(OLD_PASSWORD);

        mockMvc.perform(patch(PASSWORD_URL).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("wrong-password", NEW_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertTrue(loginAndGetToken(OLD_PASSWORD).startsWith("Bearer "));
    }

    @Test
    void newPasswordOutsideLengthRangeReturns400WithFieldError() throws Exception {
        String token = loginAndGetToken(OLD_PASSWORD);

        mockMvc.perform(patch(PASSWORD_URL).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(OLD_PASSWORD, "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.newPassword").isNotEmpty());

        mockMvc.perform(patch(PASSWORD_URL).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(OLD_PASSWORD, "x".repeat(73))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newPasswordEqualToCurrentReturns400() throws Exception {
        String token = loginAndGetToken(OLD_PASSWORD);

        mockMvc.perform(patch(PASSWORD_URL).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(OLD_PASSWORD, OLD_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(patch(PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body(OLD_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private String body(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(currentPassword, newPassword);
    }

    private String loginBody(String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private String loginAndGetToken(String password) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.data.accessToken");
    }
}
