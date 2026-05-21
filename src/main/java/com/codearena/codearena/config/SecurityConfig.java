package com.codearena.codearena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <strong>Temporary</strong> Spring Security configuration for Phase 2.
 *
 * <p>Because {@code spring-boot-starter-security} is on the classpath, Spring
 * Security locks down <em>every</em> endpoint by default and serves an HTTP
 * Basic login. That would force a username/password on every call while we are
 * still just learning REST basics.
 *
 * <p>This configuration replaces that default with a permit-all policy so the
 * REST API can be exercised freely with tools like {@code curl} or Postman.
 * It will be replaced by real, token-based authentication (JWT) in Phase 6,
 * where most endpoints become protected and only a few (login/register) stay
 * public.
 *
 * <p>CSRF protection is disabled here because this is a stateless REST API
 * consumed by non-browser clients; CSRF mainly targets cookie-based browser
 * sessions, which we do not use.
 *
 * <p>Frame options are relaxed to {@code sameOrigin} so the H2 web console
 * (enabled in Phase 4 for development) can render — it loads its UI inside
 * frames, which the default {@code DENY} policy would block.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
