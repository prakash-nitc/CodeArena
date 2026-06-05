package com.codearena.codearena.controller;

import com.codearena.codearena.dto.AuthResponse;
import com.codearena.codearena.dto.LoginRequest;
import com.codearena.codearena.dto.RegisterRequest;
import com.codearena.codearena.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints.
 *
 * <p>These live under {@code /api/auth/**}, which the security configuration
 * leaves open — you obviously can't require a token to obtain a token. Both
 * return an {@link AuthResponse} containing the JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** {@code POST /api/auth/register} — create an account and return a token (201). */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /** {@code POST /api/auth/login} — exchange credentials for a token (200, or 401). */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
