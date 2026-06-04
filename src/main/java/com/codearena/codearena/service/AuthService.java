package com.codearena.codearena.service;

import com.codearena.codearena.dto.AuthResponse;
import com.codearena.codearena.dto.LoginRequest;
import com.codearena.codearena.dto.RegisterRequest;
import com.codearena.codearena.exception.UsernameAlreadyExistsException;
import com.codearena.codearena.model.Role;
import com.codearena.codearena.model.User;
import com.codearena.codearena.repository.UserRepository;
import com.codearena.codearena.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Registration and login logic.
 *
 * <p><strong>Register:</strong> reject a taken username, hash the password with
 * BCrypt, save a {@code USER}, and immediately return a token so the client is
 * logged in.
 *
 * <p><strong>Login:</strong> delegate the credential check to Spring Security's
 * {@link AuthenticationManager} (which uses our {@code UserDetailsService} +
 * {@code PasswordEncoder}). If it throws {@code BadCredentialsException}, the
 * global handler turns that into a 401. On success we issue a fresh token.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException (-> 401) if the credentials don't match.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
