package com.freshmart.smartstock.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** A token issued BEFORE an account was locked must stop working at once, and work again after unlocking. */
class LockedUserTokenIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Test@12345";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User staff;

    @BeforeEach
    void createUsers() {
        admin = saveUser(Role.ADMIN);
        staff = saveUser(Role.STAFF);
    }

    @Test
    void lockingAUserInvalidatesHisExistingTokenImmediately() throws Exception {
        String staffToken = login(staff);
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk());

        setActive(login(admin), staff.getId(), false);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unlockingAUserMakesTheSameTokenWorkAgain() throws Exception {
        String staffToken = login(staff);
        String adminToken = login(admin);
        setActive(adminToken, staff.getId(), false);
        setActive(adminToken, staff.getId(), true);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotLockHimselfAndKeepsAccess() throws Exception {
        String adminToken = login(admin);

        mockMvc.perform(patch("/api/users/" + admin.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    private void setActive(String adminToken, Long userId, boolean active) throws Exception {
        mockMvc.perform(patch("/api/users/" + userId + "/status").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":" + active + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(active));
    }

    private User saveUser(Role role) {
        User user = new User();
        user.setFullName("Lock " + role);
        user.setEmail("lock" + System.nanoTime() + "@freshmart.test");
        user.setRole(role);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    private String login(User user) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.data.accessToken");
    }
}
