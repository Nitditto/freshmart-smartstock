package com.freshmart.smartstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. UserDetailsServiceAutoConfiguration is excluded because authentication is fully
 * handled by our own JWT filter + AuthService (no in-memory default user).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class SmartStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartStockApplication.class, args);
    }
}
