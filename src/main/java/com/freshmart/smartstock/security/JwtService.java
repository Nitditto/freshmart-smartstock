package com.freshmart.smartstock.security;

import com.freshmart.smartstock.config.AppProperties;
import com.freshmart.smartstock.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates HS256 access tokens (default lifetime 60 minutes).
 * Claims: sub (userId), email, role. The secret must come from env/secret store, >= 32 bytes.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration lifetime;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        String secret = props.jwt() == null ? null : props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (env JWT_SECRET) is missing or shorter than 32 bytes. See .env.example");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long minutes = props.jwt().expirationMinutes() > 0 ? props.jwt().expirationMinutes() : 60;
        this.lifetime = Duration.ofMinutes(minutes);
        this.clock = clock;
    }

    public String generateToken(AuthUser user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_ROLE, user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
    }

    /** Returns the identity if the token is well-formed, correctly signed and not expired; otherwise empty. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.valueOf(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return Optional.of(new AuthUser(id, email, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpiresInSeconds() {
        return lifetime.toSeconds();
    }
}
