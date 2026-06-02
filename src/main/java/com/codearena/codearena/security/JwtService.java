package com.codearena.codearena.security;

import com.codearena.codearena.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and verifies JSON Web Tokens (JWTs).
 *
 * <p>A JWT is a signed, self-contained token: it carries claims (here, the
 * username as the <em>subject</em> and the user's role) and a signature. Because
 * the server signs it with a secret and verifies that signature on every
 * request, it can trust the token's contents without any server-side session
 * store — which is what makes JWT auth <strong>stateless</strong>.
 *
 * <p>We sign with HMAC-SHA256 (symmetric: the same secret signs and verifies).
 * The secret must be at least 256 bits (32 bytes); see {@code jwt.secret}.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Issues a signed token for the given user, valid for {@code expirationMs}. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** Extracts the username (subject) from a token. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true if the token's signature and expiry are valid. */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // Covers bad signature, malformed token, expired token, null/blank input.
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
