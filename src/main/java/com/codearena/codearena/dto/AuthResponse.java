package com.codearena.codearena.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for register/login: the issued JWT plus a little context.
 *
 * <p>Clients send the token back on subsequent requests in the
 * {@code Authorization: Bearer <token>} header. {@code tokenType} ("Bearer")
 * tells them how.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String tokenType;

    private String username;

    private String role;
}
