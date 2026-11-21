package com.freshmart.smartstock.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests: full Spring context + a REAL MySQL 8 in Docker (Flyway migrations run for real).
 * Skipped automatically when Docker is not available on the machine. GitHub Actions has Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// The static container is stopped after each test class, but Spring would reuse the cached context that still
// points at the dead container's port. A fresh context per class keeps them in step.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Autowired
    protected MockMvc mockMvc;
}
