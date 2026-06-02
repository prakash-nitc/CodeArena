package com.codearena.codearena.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for {@code POST /api/auth/login}.
 *
 * <p>Only presence is validated here — the actual credential check (does this
 * username/password pair exist?) is authentication, handled by the
 * {@code AuthenticationManager}, not by Bean Validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "username must not be blank")
    private String username;

    @NotBlank(message = "password must not be blank")
    private String password;
}
