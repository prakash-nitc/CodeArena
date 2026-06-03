package com.codearena.codearena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Shared security beans, kept separate from the HTTP rules in
 * {@code SecurityConfig} so they can exist (and be used by {@code AuthService})
 * independently of how the filter chain is wired.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * Hashes passwords with BCrypt. BCrypt is deliberately slow and salts each
     * hash, which is exactly what you want for passwords (unlike fast hashes such
     * as MD5/SHA-256). Used both to encode on registration and to verify on login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes Spring Security's {@link AuthenticationManager} as a bean so
     * {@code AuthService} can authenticate login attempts. It is backed by the
     * {@code UserDetailsService} + {@code PasswordEncoder} discovered in the context.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
