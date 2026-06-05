package com.codearena.codearena.security;

import com.codearena.codearena.model.Role;
import com.codearena.codearena.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link JwtService} — no Spring context. The service is
 * constructed directly with a test secret and expiry, exercising the real
 * sign/verify round-trip.
 */
class JwtServiceTest {

    // 64-char secret -> 64 bytes, comfortably above the 32-byte HS256 minimum.
    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-0123456789abcdef";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    private User user(String username, Role role) {
        return User.builder().id(1L).username(username).password("x").role(role).createdAt(Instant.now()).build();
    }

    @Test
    void generatedToken_isValid_andCarriesUsername() {
        String token = jwtService.generateToken(user("alice", Role.USER));

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isValid_returnsFalse_forGarbageToken() {
        assertThat(jwtService.isValid("not.a.jwt")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenSignedWithADifferentSecret() {
        JwtService other = new JwtService("another-secret-another-secret-another-secret-1234567890abcdef", 3_600_000L);
        String foreignToken = other.generateToken(user("mallory", Role.ADMIN));

        // Our service must reject a token it didn't sign.
        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void expiredToken_isInvalid() {
        JwtService shortLived = new JwtService(SECRET, -1_000L); // already expired
        String token = shortLived.generateToken(user("bob", Role.USER));

        assertThat(jwtService.isValid(token)).isFalse();
    }
}
