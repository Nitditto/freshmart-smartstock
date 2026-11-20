package com.freshmart.smartstock.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.freshmart.smartstock.config.AppProperties;
import com.freshmart.smartstock.domain.enums.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";
    private static final Instant NOW = Instant.parse("2026-11-20T03:00:00Z");

    private static AppProperties props(String secret) {
        return new AppProperties("Asia/Ho_Chi_Minh",
                new AppProperties.Jwt(secret, 60),
                new AppProperties.Llm("", "", "", 30),
                new AppProperties.Seed(false, ""));
    }

    private static JwtService serviceAt(String secret, Instant instant) {
        return new JwtService(props(secret), Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void generatedTokenCanBeParsedBack() {
        JwtService jwt = serviceAt(SECRET, NOW);
        String token = jwt.generateToken(new AuthUser(7L, "staff@freshmart.local", Role.STAFF));

        Optional<AuthUser> parsed = jwt.parse(token);

        assertTrue(parsed.isPresent());
        assertEquals(7L, parsed.get().id());
        assertEquals("staff@freshmart.local", parsed.get().email());
        assertEquals(Role.STAFF, parsed.get().role());
    }

    @Test
    void tokenIsRejectedAfter60Minutes() {
        String token = serviceAt(SECRET, NOW).generateToken(new AuthUser(1L, "a@b.c", Role.ADMIN));

        assertTrue(serviceAt(SECRET, NOW.plusSeconds(59 * 60)).parse(token).isPresent());
        assertTrue(serviceAt(SECRET, NOW.plusSeconds(61 * 60)).parse(token).isEmpty());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = serviceAt(SECRET, NOW).generateToken(new AuthUser(1L, "a@b.c", Role.ADMIN));

        assertTrue(serviceAt("another-secret-another-secret-123456789", NOW).parse(token).isEmpty());
    }

    @Test
    void garbageTokenIsRejected() {
        assertTrue(serviceAt(SECRET, NOW).parse("not.a.jwt").isEmpty());
    }

    @Test
    void secretShorterThan32BytesFailsFast() {
        assertThrows(IllegalStateException.class, () -> serviceAt("too-short", NOW));
    }
}
